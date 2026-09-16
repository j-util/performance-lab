#!/usr/bin/env python3
"""Preserve every single-shot sample; summarize JMH output without filtering."""
import csv
import json
import math
from pathlib import Path
import re
import statistics
import sys

root = Path(sys.argv[1])
summaries = []
samples = []
fork_rows = []
gc_rows = []

def percentile(values, fraction):
    ordered = sorted(values)
    rank = (len(ordered) - 1) * fraction
    low = math.floor(rank)
    high = math.ceil(rank)
    return ordered[low] + (ordered[high] - ordered[low]) * (rank - low)

def write_csv(name, rows):
    with (root / name).open('w', newline='') as output:
        writer = csv.DictWriter(output, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

for run, expected in [('destination-primary', 20), ('destination-repeat', 10), ('hardwood', 6)]:
    entries = json.loads((root / (run + '.json')).read_text())
    assert len(entries) == expected, (run, len(entries))
    for entry in entries:
        assert entry['mode'] == 'ss' and entry['forks'] == 3 and entry['threads'] == 1
        assert entry['warmupIterations'] == 5 and entry['measurementIterations'] == 10
        metric = entry['primaryMetric']
        raw = metric['rawData']
        assert len(raw) == 3 and all(len(fork) == 10 for fork in raw)
        secondary = entry['secondaryMetrics']
        allocations = secondary['gc.alloc.rate.norm']['rawData']
        collections = secondary['gc.count']['rawData']
        key = dict(run=run, method=entry['benchmark'].rsplit('.', 1)[-1],
                   rows=int(entry['params']['rowCount']), batch=int(entry['params']['batchSize']))
        values = [v for fork in raw for v in fork]
        means = [statistics.mean(fork) for fork in raw]
        summaries.append(dict(**key, mean_ms=metric['score'], jmh_99_9_ci_low_ms=metric['scoreConfidence'][0],
            jmh_99_9_ci_high_ms=metric['scoreConfidence'][1], jmh_99_9_half_width_ms=metric['scoreError'],
            median_ms=statistics.median(values), p90_ms=percentile(values, .9), min_ms=min(values), max_ms=max(values),
            fork1_ms=means[0], fork2_ms=means[1], fork3_ms=means[2],
            allocation_bytes_per_op=secondary['gc.alloc.rate.norm']['score'],
            allocation_mib_per_op=secondary['gc.alloc.rate.norm']['score']/1048576,
            measured_gc_count=sum(v for fork in collections for v in fork),
            samples_with_gc=sum(v > 0 for fork in collections for v in fork)))
        for f, values_in_fork in enumerate(raw):
            fork_rows.append(dict(**key, fork=f+1, mean_ms=means[f], median_ms=statistics.median(values_in_fork),
                                  p90_ms=percentile(values_in_fork,.9)))
            for i, value in enumerate(values_in_fork):
                samples.append(dict(**key, fork=f+1, iteration=i+1, time_ms=value,
                    allocation_bytes=allocations[f][i], gc_count=collections[f][i]))

    # Associate console fork order with log completion order: benchmarks run serially.
    launches=[]
    current_method=None
    current_params={}
    for line in (root/(run+'.txt')).read_text().splitlines():
        if line.startswith('# Benchmark: '): current_method=line.split(': ',1)[1].rsplit('.',1)[-1]
        if line.startswith('# Parameters: '):
            current_params={k:int(v) for k,v in re.findall(r'(rowCount|batchSize) = (\d+)',line)}
        match=re.match(r'# Fork: (\d+) of 3$',line)
        if match: launches.append((current_method,current_params.copy(),int(match[1])))
    logs=sorted((root/'gc').glob(run+'-*.txt'),key=lambda p:p.stat().st_mtime_ns)
    assert len(logs)==len(launches)==expected*3,(run,len(logs),len(launches))
    for path,(method,params,fork) in zip(logs,launches):
        lines=path.read_text().splitlines()
        forced=[]
        pauses=[]
        starts={}
        for index,line in enumerate(lines):
            match=re.search(r'GC\((\d+)\) (Pause.*?) (\d+(?:\.\d+)?)ms$',line)
            if match:
                event=dict(line=index+1,gc_id=int(match[1]),kind=match[2],pause_ms=float(match[3]))
                if 'System.gc()' in match[2]: forced.append(event)
                else: pauses.append(event)
            if 'gc,start' in line:
                match=re.search(r'GC\((\d+)\)',line)
                if match: starts[int(match[1])]=index+1
        assert len(forced)==30,(path,len(forced))
        for iteration in range(15):
            lower=forced[2*iteration+1]['line']
            upper=starts[forced[2*iteration+2]['gc_id']] if iteration<14 else len(lines)+1
            in_window=[event for event in pauses if lower<event['line']<upper]
            gc_rows.append(dict(run=run,method=method,rows=params['rowCount'],batch=params['batchSize'],fork=fork,
                phase='warmup' if iteration<5 else 'measurement',iteration=iteration+1 if iteration<5 else iteration-4,
                log=str(path.relative_to(root)),after_cleanup_line=lower,before_next_cleanup_line=upper,
                non_forced_pause_count=len(in_window),non_forced_pause_ms=sum(event['pause_ms'] for event in in_window),
                events=json.dumps(in_window,separators=(',',':'))))

write_csv('summary.csv', summaries)
write_csv('per-invocation.csv', samples)
write_csv('per-fork.csv', fork_rows)
write_csv('gc-windows.csv', gc_rows)
(root/'summary.json').write_text(json.dumps(summaries,indent=2)+'\n')
for run in ['destination-primary','destination-repeat','hardwood']:
    lines=['| Rows | Batch | Method | Mean ms ± JMH 99.9% CI | Median | p90 | Fork means ms | MiB/op | GC samples / 30 |',
           '|---:|---:|---|---:|---:|---:|---|---:|---:|']
    for s in sorted((r for r in summaries if r['run']==run),key=lambda r:(r['rows'],r['batch'],r['method'])):
        lines.append(f"| {s['rows']:,} | {s['batch']:,} | {s['method']} | {s['mean_ms']:.3f} ± {s['jmh_99_9_half_width_ms']:.3f} | {s['median_ms']:.3f} | {s['p90_ms']:.3f} | {s['fork1_ms']:.3f}, {s['fork2_ms']:.3f}, {s['fork3_ms']:.3f} | {s['allocation_mib_per_op']:.3f} | {s['samples_with_gc']} |")
    (root/(run+'-table.md')).write_text('\n'.join(lines)+'\n')
print(f'PASS {len(summaries)} cases, {len(fork_rows)} forks, {len(samples)} untrimmed measured invocations, {len(gc_rows)} GC windows')
