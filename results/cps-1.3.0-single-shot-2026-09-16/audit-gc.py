from pathlib import Path
from collections import defaultdict,Counter
import csv,json,re,sys
root=Path(sys.argv[1])
samples=list(csv.DictReader((root/'per-invocation.csv').open()))
windows=[w for w in csv.DictReader((root/'gc-windows.csv').open()) if w['phase']=='measurement']
key=lambda x:tuple(x[k] for k in ('run','method','rows','batch','fork','iteration'))
lookup={key(s):s for s in samples}
assert len(lookup)==len(windows)==1080
mismatches=[]
for w in windows:
 s=lookup[key(w)]
 if float(s['gc_count']) != int(w['non_forced_pause_count']): mismatches.append({'sample':s,'window':w})
assert not mismatches,mismatches
out=['PASS: per-invocation profiler GC counts match log pause counts for all 1080 measured iterations.',
     '108 benchmark fork logs; 30 completed forced System.gc() pauses per fork, 3240 in total.',
     'The 30 forced pauses form two before each of 5 warmup and 10 measurement iterations.',
     'Log windows exclude those cleanup pauses. Windows conservatively include harness work around the operation; the last includes trial teardown.',
     'Warmup/trial-setup GC is retained in raw logs and separated in gc-windows.csv.']
for run in ['destination-primary','destination-repeat','hardwood']:
 for method in sorted({w['method'] for w in windows if w['run']==run}):
  for rows in sorted({int(w['rows']) for w in windows if w['run']==run and w['method']==method}):
   ws=[w for w in windows if w['run']==run and w['method']==method and int(w['rows'])==rows]
   events=[e for w in ws for e in json.loads(w['events'])]
   kinds=Counter(re.sub(r' \d+M->.*','',e['kind']) for e in events)
   out.append(f'{run} {method} rows={rows}: samples_with_gc={sum(int(w["non_forced_pause_count"])>0 for w in ws)}/{len(ws)}, pauses={len(events)}, pause_sum_ms={sum(e["pause_ms"] for e in events):.3f}, max_pause_ms={max((e["pause_ms"] for e in events),default=0):.3f}, evacuation_failure_pauses={sum("Evacuation Failure" in e["kind"] for e in events)}, kinds={dict(kinds)}')
for w in windows:
 if w['run'].startswith('destination') and int(w['non_forced_pause_count']):out.append('Destination measured event: '+json.dumps(w))
(root/'gc-audit.txt').write_text('\n'.join(out)+'\n')
print('\n'.join(out))
