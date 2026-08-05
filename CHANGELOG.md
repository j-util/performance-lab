# Changelog

All notable changes to this repository will be documented in this file.

## Unreleased

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
