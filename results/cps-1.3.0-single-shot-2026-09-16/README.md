# CPS 1.3 materialization follow-up — single-shot evidence, 2026-09-16

**Verdict: FUNCTIONALLY READY; PERFORMANCE TRADEOFF CHARACTERIZED.**

No release blocker is established by the corrected measurements. The actual
Hardwood executor path shows no substantial repeatable regression against the
typed-batch consumer path. No universal speedup or version-to-version performance
claim is made.

This follow-up corrects the measurement boundary and release interpretation of
[the historical report](../cps-1.3.0-2026-09-16/README.md). Repeated one-second
`AverageTime` loops allocated roughly 500–650 MiB per operation, allowing GC
placement to dominate scores. Longer warmup alone would not correct that
boundary. The earlier raw evidence and report body are preserved. Its requirement
that the appender demonstrate a stable win over already prepared typed batches
was too broad for release readiness.

## Revisions and functional evidence

- Measured performance-lab source: `41afe3444622e9a46d3f0bf2d9f5e9d2e6fadcee`.
- Previous performance-lab evidence: `389be7e3ca1c77c1bc1ba5eac055fe1f967e542f`.
- Read-only CPS candidate: `5f0761b99d14be932e4890795d4007c7aeb2013c`.
- Read-only Hardwood consumer: `8563cc5184ecdc4c933e55884303639b6252ce09`.

The benchmark/methodology commit was pushed before the exact-commit clean build
and all three JMH runs. This subsequent commit adds evidence and report links
only. All three worktrees started clean; no applicable `AGENTS.md` was found in
the repositories or their ancestors. No unrelated work was changed.

The unchanged CPS candidate's correctness, external published-1.2 compatibility,
Java 8/26 consumer tests and exact-SHA CI remain the historical functional
basis; those CPS/Hardwood release checks were not rerun or expanded here.
Fresh performance-lab `clean verify` passed 67 tests, including actual Parquet
loader correctness and executor ownership. The separate untimed validator
passed all 20 destination combinations: both row counts, both chunk sizes and
all five selected paths. It visited 101,000,000 rows and compared all eight
fields at their source positions (808,000,000 comparisons). It also checked
size, exact store/ArrayList capacity, sealed state and rejected post-seal writes
for stores, and that the borrowed executor remained usable. ArrayList has no
sealing contract. Reflection/module opening is confined to this untimed validator.

`dependency-provenance.json` and `artifact-sha256.txt` identify the actual locally
installed artifacts. Both CPS JAR hashes match the historical evidence. Every
ZIP entry of all four installed CPS/Hardwood runtime/processor JARs matches the
corresponding existing worktree build; CPS ZIP container hashes differ from the
last worktree build but entry contents do not. No CPS or Hardwood build, install,
source modification, new commit or push was performed in this follow-up.

## Environment and measurement boundary

Native Apple M4, 10 physical/logical CPUs, 24 GiB RAM; macOS 26.6.2 (25G83);
Temurin 25.0.2+10-LTS aarch64; JMH 1.37; Maven Wrapper 3.9.16. Each fork uses
`-Xms4g -Xmx4g -XX:+UseG1GC`. The start snapshot reported battery power (28%); the ending snapshot reported
AC power while the battery was still discharging (21%). The transition timing
and its effect were not recorded, so power state is an uncontrolled limitation.
This is one desktop environment, not an isolated hardware performance guarantee.
Full observed environment and ending power state are retained.

Both suites use `-bm ss -wi 5 -i 10 -f 3 -t 1 -gc true -prof gc` (time unit ms),
with single-shot batch size one: one fresh completed destination per iteration.
Each case has 30 measured invocations across three fresh JVMs. The primary
matrix has 20 cases (600 measurements), its independent complete 10M repeat has
10 (300), and the actual end-to-end suite has six (180). Runs were sequential;
no compilation or validation ran concurrently with timed measurements.

Destination trial setup creates and retains the same deterministic eight source
arrays (two long, three double, three String) and creates the executors. Every
measured operation allocates an exact-capacity destination, copies ranges or
constructs every immutable row, seals a columnar store, and returns it to JMH.
Executor task creation, scheduling and completion waits remain inside timing.
The source string references are reused; the benchmark does not copy strings.

The two executor cases differ deliberately:

- **Per-batch barrier** submits eight column copies and waits for completion
  before the next range. It matches the current Hardwood loader's scheduling
  boundary. The destination case uses CompletableFuture; the actual generated
  loader uses FutureTask and waits for every submitted task, including failure
  handling. The end-to-end benchmark executes that actual loader.
- **Experimental pipelined** appends each range to an ordered future chain per
  column, then joins the eight final tails once before sealing. This is best-case
  API usage when all arrays are retained and available, not the current Hardwood
  implementation. Its numbers must not be labeled as actual Hardwood performance.

End-to-end setup writes the existing two uncompressed, dictionary-disabled
Parquet files outside timing and creates the copy executor. Opening both files,
metadata-based exact capacity, column reader construction, decoding, crossing
the file boundary, materialization, sealing and resource closing remain inside
measurement. All three paths use the existing 1,000,000-row loader batch size.
Repeated reads benefit from the OS page cache. These deterministic low-cardinality
strings, schema, file encoding and row counts are the scope of the evidence.

Explicit cleanup defines isolated materialization after reclamation. It does
not estimate sustained allocation throughput or production latency under a
different heap/collector. Typed batches remain the natural path for already
prepared aligned arrays; `ColumnAppender` adds independently produced columns
and caller-managed concurrency. Allocation is volume per operation, not retained
heap, and includes worker threads plus small JMH/profiler overhead. End-to-end
allocation also includes decoding; final trial teardown may contribute small
profiler overhead although it is outside the primary timer.

## Commands and verification

Run from performance-lab. The exact expanded commands and shell output are in
`driver.txt` and `continuation-driver.txt`; all benchmark console output and raw
JSON are retained separately.
The complete sequence was invoked with:

```sh
JAVA_HOME=/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem \
  bash scripts/cps13/run-evidence.sh /private/tmp/cps13-single-shot-2026-09-16
```

The original command session became unavailable during the independent repeat.
Process inspection confirmed the repeat continued; its complete JSON and console
output were preserved. The driver ended before launching Hardwood. The remaining
end-to-end command, analysis and diff check were then run with the identical
artifact and flags via `finish-evidence.sh`. No destination run or sample was
replaced; `runner-continuation.txt` records the boundary and artifact hash.

The script exports `$JAVA_HOME/bin` at the front of PATH. Its verification and
benchmark commands are reproduced in [commands.md](commands.md), including the
fixed heap, process/time-based GC output names and full selectors. Before the
first commit, `JAVA_HOME=... ./mvnw -B -ntp verify`, validator compilation and the
same full-value validator also passed. The exact source was rebuilt with
`./mvnw -B -ntp clean verify` after push. The focused shaded-JAR listing contained
exactly the five intended destination methods. Existing virtual-thread methods
remain available outside this selected matrix. `git diff --check` passed after
measurement and again after recording the evidence. The first staged whitespace
check flagged original Maven/JMH spaces, JSON final blank lines and CSV CRLF
separators. Evidence-local `.gitattributes` permits only those tool-output formats
without changing their bytes; source and report whitespace checks remain active.
`initial-whitespace-check.txt` records the initial findings.

## Statistics

All samples are retained; no outlier trimming, favorable-fork selection or
post-result chunk-size selection was used. Tables report arithmetic means and
JMH's 99.9% confidence half-width, the median and p90 across all 30 measured
invocations, three separate fork means, and normalized allocation in MiB/op
(B/op divided by 1,048,576). The p90 uses linear interpolation at `(n-1)*0.9`.
Raw JSON also retains normalized allocation confidence intervals and every
profiler sample. `per-invocation.csv`, `per-fork.csv`, `summary.csv` and
`summary.json` expose the values used for tables. JMH's pooled interval does not
remove within-fork correlation; three forks and 30 samples are not a universal
latency or tail guarantee. Primary and independent repeat results remain separate.

## Primary destination-only results

| Rows | Batch | Method | Mean ms ± JMH 99.9% CI | Median | p90 | Fork means ms | MiB/op | GC samples / 30 |
|---:|---:|---|---:|---:|---:|---|---:|---:|
| 100,000 | 8,192 | arrayListRows | 2.234 ± 0.365 | 2.279 | 2.843 | 2.264, 2.280, 2.159 | 6.492 | 0 |
| 100,000 | 8,192 | columnarFixedPoolPerBatchBarrierAppender | 1.772 ± 0.430 | 1.718 | 1.971 | 1.662, 1.897, 1.758 | 4.990 | 0 |
| 100,000 | 8,192 | columnarFixedPoolPipelinedAppender | 1.260 ± 0.187 | 1.316 | 1.487 | 1.326, 1.232, 1.222 | 4.980 | 0 |
| 100,000 | 8,192 | columnarSequentialRangedBatches | 1.339 ± 0.178 | 1.384 | 1.565 | 1.260, 1.430, 1.327 | 4.967 | 0 |
| 100,000 | 8,192 | columnarSingleThreadedAppender | 1.372 ± 0.136 | 1.415 | 1.558 | 1.373, 1.448, 1.294 | 4.966 | 0 |
| 100,000 | 1,000,000 | arrayListRows | 1.792 ± 0.325 | 1.785 | 2.515 | 1.594, 1.778, 2.005 | 6.492 | 0 |
| 100,000 | 1,000,000 | columnarFixedPoolPerBatchBarrierAppender | 1.165 ± 0.164 | 1.169 | 1.417 | 1.184, 1.169, 1.143 | 4.969 | 0 |
| 100,000 | 1,000,000 | columnarFixedPoolPipelinedAppender | 1.212 ± 0.104 | 1.237 | 1.360 | 1.270, 1.212, 1.152 | 4.969 | 0 |
| 100,000 | 1,000,000 | columnarSequentialRangedBatches | 1.310 ± 0.120 | 1.324 | 1.514 | 1.328, 1.280, 1.323 | 4.966 | 0 |
| 100,000 | 1,000,000 | columnarSingleThreadedAppender | 1.021 ± 0.286 | 1.130 | 1.506 | 0.864, 1.033, 1.166 | 4.966 | 0 |
| 10,000,000 | 8,192 | arrayListRows | 229.912 ± 13.914 | 225.056 | 242.092 | 236.151, 227.450, 226.136 | 648.505 | 1 |
| 10,000,000 | 8,192 | columnarFixedPoolPerBatchBarrierAppender | 50.393 ± 4.323 | 50.153 | 59.163 | 53.574, 49.127, 48.479 | 498.062 | 0 |
| 10,000,000 | 8,192 | columnarFixedPoolPipelinedAppender | 33.957 ± 2.718 | 32.766 | 41.247 | 34.807, 35.208, 31.857 | 497.096 | 0 |
| 10,000,000 | 8,192 | columnarSequentialRangedBatches | 47.938 ± 3.955 | 48.903 | 50.360 | 48.511, 47.583, 47.721 | 496.002 | 0 |
| 10,000,000 | 8,192 | columnarSingleThreadedAppender | 37.872 ± 11.066 | 31.571 | 50.846 | 43.680, 40.113, 29.824 | 495.918 | 0 |
| 10,000,000 | 1,000,000 | arrayListRows | 232.324 ± 9.325 | 229.463 | 247.078 | 225.725, 229.785, 241.463 | 648.505 | 0 |
| 10,000,000 | 1,000,000 | columnarFixedPoolPerBatchBarrierAppender | 29.625 ± 2.644 | 30.175 | 32.581 | 28.729, 30.904, 29.244 | 495.937 | 0 |
| 10,000,000 | 1,000,000 | columnarFixedPoolPipelinedAppender | 27.465 ± 2.082 | 28.535 | 29.804 | 25.931, 27.875, 28.588 | 495.929 | 0 |
| 10,000,000 | 1,000,000 | columnarSequentialRangedBatches | 53.535 ± 4.999 | 53.400 | 59.105 | 52.913, 50.161, 57.532 | 495.918 | 0 |
| 10,000,000 | 1,000,000 | columnarSingleThreadedAppender | 41.586 ± 7.376 | 44.141 | 54.126 | 52.983, 37.788, 33.987 | 495.918 | 0 |

## Independent complete 10M destination repeat

| Rows | Batch | Method | Mean ms ± JMH 99.9% CI | Median | p90 | Fork means ms | MiB/op | GC samples / 30 |
|---:|---:|---|---:|---:|---:|---|---:|---:|
| 10,000,000 | 8,192 | arrayListRows | 243.062 ± 17.031 | 233.658 | 276.518 | 259.611, 236.363, 233.212 | 648.505 | 1 |
| 10,000,000 | 8,192 | columnarFixedPoolPerBatchBarrierAppender | 48.788 ± 4.089 | 48.629 | 55.463 | 49.414, 47.391, 49.560 | 498.064 | 0 |
| 10,000,000 | 8,192 | columnarFixedPoolPipelinedAppender | 31.833 ± 5.860 | 32.134 | 37.110 | 33.954, 31.124, 30.420 | 497.090 | 0 |
| 10,000,000 | 8,192 | columnarSequentialRangedBatches | 48.152 ± 9.117 | 48.399 | 50.563 | 45.315, 51.323, 47.818 | 496.002 | 0 |
| 10,000,000 | 8,192 | columnarSingleThreadedAppender | 48.215 ± 4.555 | 48.356 | 54.751 | 45.635, 49.094, 49.914 | 495.918 | 0 |
| 10,000,000 | 1,000,000 | arrayListRows | 231.270 ± 13.487 | 226.153 | 249.703 | 228.790, 225.606, 239.415 | 648.505 | 0 |
| 10,000,000 | 1,000,000 | columnarFixedPoolPerBatchBarrierAppender | 30.117 ± 4.981 | 29.285 | 35.304 | 28.955, 28.791, 32.606 | 495.937 | 0 |
| 10,000,000 | 1,000,000 | columnarFixedPoolPipelinedAppender | 26.412 ± 2.581 | 27.466 | 29.530 | 25.457, 25.700, 28.079 | 495.929 | 0 |
| 10,000,000 | 1,000,000 | columnarSequentialRangedBatches | 50.474 ± 5.843 | 52.240 | 57.360 | 49.065, 50.236, 52.122 | 495.918 | 0 |
| 10,000,000 | 1,000,000 | columnarSingleThreadedAppender | 52.479 ± 8.447 | 51.606 | 58.571 | 50.602, 53.077, 53.759 | 495.918 | 0 |

## Actual end-to-end Hardwood results

| Rows | Batch | Method | Mean ms ± JMH 99.9% CI | Median | p90 | Fork means ms | MiB/op | GC samples / 30 |
|---:|---:|---|---:|---:|---:|---|---:|---:|
| 100,000 | 1,000,000 | hardwoodToArrayList | 18.173 ± 3.112 | 20.034 | 21.995 | 20.592, 18.766, 15.162 | 462.253 | 0 |
| 100,000 | 1,000,000 | hardwoodToColumnarBatch | 19.122 ± 2.681 | 20.603 | 22.673 | 20.459, 17.642, 19.265 | 460.688 | 0 |
| 100,000 | 1,000,000 | hardwoodToExecutorBackedColumnarBatch | 18.677 ± 2.709 | 19.617 | 22.327 | 17.950, 19.897, 18.185 | 460.749 | 0 |
| 10,000,000 | 1,000,000 | hardwoodToArrayList | 760.920 ± 77.754 | 724.046 | 943.606 | 843.840, 761.409, 677.512 | 5055.584 | 30 |
| 10,000,000 | 1,000,000 | hardwoodToColumnarBatch | 530.351 ± 47.433 | 510.328 | 602.621 | 567.487, 515.148, 508.418 | 4903.035 | 30 |
| 10,000,000 | 1,000,000 | hardwoodToExecutorBackedColumnarBatch | 524.251 ± 49.355 | 495.986 | 584.380 | 530.100, 517.775, 524.877 | 4903.038 | 30 |

## GC verification

The current JDK accepted process/time substitutions `%p-%t` in the unified-log
filename. All 108 benchmark fork logs and the separate filename probe are
retained under `gc/`; no log rotation was enabled. Each fork records exactly
30 completed forced `Pause Full (System.gc())` collections: two before each of
five warmup and ten measurement iterations, 3,240 total. Inspection of the
installed JMH 1.37 source confirms these forced collections precede
`runIteration`; its GC profiler snapshots are taken within `runIteration`.
The relevant installed source excerpts are retained in `jmh-gc-boundary-source.txt`.

`gc-windows.csv` maps every warmup/measurement iteration to its fork log and line
boundaries between completed forced cleanup and the next cleanup. The filename
mapping follows serial console fork order and log completion order. The windows
conservatively include harness work around the operation and last-trial teardown;
they are not nanosecond entry/exit traces of the benchmark method. Crucially,
**log pause counts and profiler GC counts agree for all 1,080 measured iterations**
(`gc-audit.txt`). Trial setup and warmup collections are kept in the raw logs and
are not misclassified as measurement collections.

- **Destination columnar:** zero measured collections across all 720 invocations
  in the primary and repeat runs, corroborated by no non-forced pause in their
  measurement windows.
- **Destination ArrayList:** two GC-affected invocations out of 180, both at
  10M rows / 8,192 chunks. Primary fork 3, iteration 9: 73.961 ms young pause
  (`destination-primary-79801-2026-09-16_22-54-48.txt`, line 684); repeat fork 2,
  iteration 1: 54.991 ms (`destination-repeat-82957-2026-09-16_23-04-42.txt`,
  line 380). JMH reports rounded GC times of 73 and 55 ms. Both samples are retained.
- **End-to-end 100K:** zero measured collections across all 90 invocations.
- **End-to-end 10M:** every one of the 90 invocations incurred collections;
  254 total. Decoding plus destination allocation exceeds the fixed heap per
  operation in allocation volume, so explicit cleanup cannot make this path
  GC-free. Those costs remain inside its reported score.

| End-to-end 10M path | Measured collections / 30 invocations | Total logged pause ms | Maximum pause ms | Pauses reporting evacuation failure |
|---|---:|---:|---:|---:|
| ArrayList | 81 | 10,277.741 | 319.029 | 24 |
| Typed batches | 91 | 5,765.609 | 276.862 | 10 |
| Actual per-batch executor | 82 | 5,463.563 | 215.282 | 11 |

The logs include humongous-allocation-triggered young collections, G1
`Evacuation Failure: Allocation` events and one ArrayList remark pause. These
are observed GC events under the prescribed 4 GiB heap, not failed benchmark
invocations. All matrices completed. GC-sensitive end-to-end comparisons must
therefore remain specific to this heap, collector and data set.

## Interpretation and limitations

**FUNCTIONALLY READY; PERFORMANCE TRADEOFF CHARACTERIZED.** The unchanged
candidate retains the historical functional/compatibility evidence, and the
fresh lab build, actual-loader tests and exhaustive destination validator pass.
The corrected measurements do not establish a substantial repeatable regression
in the intended consumer path. No arbitrary acceptance percentage was imposed.

At 10M rows, actual Hardwood typed batches average 530.351 ms/op and the current
per-batch executor loader 524.251 ms/op, with overlapping 99.9% intervals. The
nominal executor difference is -1.2%; this is not an established speedup or a
formal equivalence result. The three executor fork means are 530.100, 517.775
and 524.877 ms/op. At 100K rows the means (19.122 versus 18.677 ms/op) likewise
overlap. These observations provide no basis for the earlier broad blocker.

ArrayList averages 760.920 ms/op at 10M in this consumer fixture, versus roughly
524–530 ms/op for the columnar paths. At 100K the three intervals overlap.
End-to-end columnar allocation is about 4,903 MiB/op versus 5,056 MiB/op for
ArrayList; decoding dominates this allocation total. Typed batches already
share the columnar allocation benefit. This one end-to-end matrix has three
forks but was not independently repeated as a whole, and no published-1.2
end-to-end performance baseline was collected. These are matched API-path
comparisons on the candidate, not a proof about every workload or past version.

Destination-only results characterize copying and executor overhead. Small
8,192-row chunks give the barrier path roughly 49–50 ms/op at 10M in both runs,
close to typed batches at roughly 48 ms/op. At 1,000,000-row chunks its roughly
30 ms/op mean is lower than typed batches' roughly 50–54 ms/op here. Experimental
pipelining is roughly 26–34 ms/op across the two chunks/runs; that benefit cannot
be attributed to the current Hardwood loader. It depends on already retained
source arrays and ordered caller scheduling.

The synchronous appender remains variable despite no measured collections:
37.872 to 48.215 ms/op (+27.3%) between runs at 8,192 chunks, and 41.586 to
52.479 ms/op (+26.2%) at 1,000,000. Therefore it does not demonstrate a repeatable
advantage over typed batches; no unmeasured cause is assigned to this drift.
The remaining variation, only five warmup shots, fixed benchmark ordering,
uncontrolled desktop activity/power state and one machine/JDK limit broader
performance claims. All samples, including slow samples, contribute to the tables.

Typed batches remain the natural choice for aligned arrays already available.
The appender's contribution is independently produced columns and caller-managed
concurrency. It is not required to win this already-aligned copying fixture.
No CPS/Hardwood optimization, redesign or integration change follows from these
measurements.

## Evidence inventory and delivery scope

- `destination-primary.json`, `destination-repeat.json`, `hardwood.json` and
  matching `.txt` console files: complete, unmodified JMH outputs.
- `gc/`: all 108 process-specific logs plus the filename probe;
  `gc-windows.csv`, `gc-audit.txt`, `audit-gc.py`: per-iteration reconciliation.
- `per-invocation.csv`, `per-fork.csv`, `summary.csv`, `summary.json`, and the
  three `*-table.md` files: untrimmed derived statistics. Reproduce with the
  committed `scripts/cps13/summarize.py` and retained `audit-gc.py`.
- `commands.md`, `driver.txt`, `continuation-driver.txt`, `finish-evidence.sh`,
  `runner-continuation.txt`: commands and the documented runner continuation.
- `clean-verify.txt`, `destination-list.txt`, `destination-validation.txt`,
  `precommit-verify.txt`, `precommit-validator.txt`, `diff-check.txt`,
  `final-diff-check.txt`: verification evidence.
- `environment.txt`, `environment-end.txt`, `artifact-sha256.txt`,
  `dependency-provenance.json`, `scope-audit.json`, `audit-scope.py`,
  `jmh-gc-boundary-source.txt`: provenance, unchanged-consumer and historical
  preservation evidence. `preflight.json` is the initial tracked-file fingerprint.
- `SHA256SUMS`: hashes of retained evidence files, excluding itself and the
  subsequently written final diff-check result.

CPS remains at `5f0761b99d14be932e4890795d4007c7aeb2013c` and Hardwood at
`8563cc5184ecdc4c933e55884303639b6252ce09`, with unchanged tracked contents and
clean worktrees. The original report body and every original raw file remain
available; only a prominent supersession note/link was added to that report.
This follow-up changes only performance-lab. No artifact was published, no
version was changed, no tag or GitHub release was created, and no issue was
closed. A corrective follow-up comment links both reports on CPS issue #7;
the earlier comment remains intact.
