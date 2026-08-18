# Changelog

All notable changes to this repository will be documented in this file.

## Unreleased

- Use explicit, identical Hardwood decoding batch sizes across all materialization
  benchmark paths.
- Add a separate ready-data iterator benchmark comparing an exactly pre-sized
  `ArrayList<Item>` with one- and ten-segment `SpliceList<Item>` representations
  over the same deterministic 10-million-item fixture.
- Replace the CSV-backed collection-growth experiments with a pure append
  benchmark whose primary comparison gives `ArrayList` initial capacity and
  `SpliceList` segment size the same six values before the same ordinary-add
  loop. Keep default-growing and exact-final-capacity `ArrayList` plus optimized
  `SpliceList.addLast` as clearly labelled contextual baselines across a
  six-by-six matrix extending through 30 million elements and a 30,000-element
  capacity hint.
- Add a collection-only parallel fill-and-combine benchmark with deterministic
  partitions, worker-owned local lists, exactly matched local capacity
  knowledge, and deterministic `addAll` or destructive `spliceTail`
  consolidation, plus invocation-prepared merge-only methods.
- Use the released `splice-list:2.0.0` benchmark dependency instead of the local
  `2.0.0-SNAPSHOT` artifact.
- Add a focused Columnar Projection Store iteration benchmark comparing its
  reusable cursor, indexed stable views, and OO-style `forEach` traversal for
  narrow-field work and full-row checksums, including allocation profiling
  guidance.
- Add complete eight-field DFLib DataFrame and manually assembled HPPC primitive
  column comparators to the ready market-data snapshot average suite, including
  DFLib native and ordinary-addition calculations.
- Add MemorySegment rows, Chronicle Values over direct Chronicle Bytes rows,
  and Apache Arrow vectors to the complete-snapshot ready-data average suite,
  with deterministic native-resource teardown and correctness coverage.
- Add a separate ready-data average benchmark over complete eight-field market
  data snapshots while retaining a calculation-only last-trade-price baseline.
- Add ordinary-addition Eclipse Collections and Tablesaw variants alongside
  their native price-average operations.
- Simplify and rename the primitive price-average baseline as a price-only
  `double[]` calculation baseline.
- Measure the ready-data price-average suite as repeated average-time traversal
  in microseconds per operation, with one-second warmup and measurement
  iterations while keeping construction outside measurement.
- Add four configurable default sizes to the ready-data price-average benchmark
  and a manually dispatched workflow for running one benchmark suite at a time.
- Add an in-memory average-price benchmark across complete-record JDK,
  FastUtil, Eclipse Collections, Tablesaw, and Columnar Projection Store
  representations.
- Split the CSV benchmarks into four classes grouped by directly comparable operations.
- Restructure the benchmark project into shared core and JMH runner modules.
- Add the initial Maven, JMH, and JUnit 5 project structure.
- Add deterministic, streaming CSV dataset generation for future benchmarks.
