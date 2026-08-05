# j-util Performance Lab

This repository contains reproducible benchmarks and performance experiments for
j-util libraries. It is a benchmark and demo workspace, not a reusable library.

Benchmark results depend on the hardware, operating system, JDK, JVM options, and
runtime conditions under which they are collected. Results from this repository
must not be treated as universal performance claims.

## Requirements

- JDK 17 or newer

The Maven Wrapper downloads the project's Maven version automatically.

## Modules

- `benchmark-core` contains the shared benchmark models, deterministic data
  generation, workload implementations, and correctness tests.
- `benchmark-jmh` contains the OpenJDK JMH benchmark classes and produces the
  executable benchmark JAR.

## Build and test

```shell
./mvnw clean verify
```

Ordinary verification tests belong under `benchmark-core/src/test/java` and use
JUnit 5.

## Generate benchmark data

Dataset generation is a standalone step and is never performed as part of a
measured benchmark operation. Build the self-contained JAR, then pass the desired
row count to the generator:

```shell
./mvnw clean package
java -cp benchmark-jmh/target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 1000000
```

This example deterministically writes 1,000,000 data rows plus a header to
`target/benchmark-data/benchmark-rows-1000000.csv`. Repeating the command with
the same row count produces identical CSV data. To choose another destination,
pass it as the second argument:

```shell
java -cp benchmark-jmh/target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 1000 /tmp/benchmark.csv
```

Generated files under `target/` are build artifacts and must not be committed.

For a quick development run, generate the benchmark's default 10,000-row dataset:

```shell
java -cp benchmark-jmh/target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 10000
```

## Run the benchmarks

The original 15 benchmark methods are organized into four classes so that each
class contains only directly comparable operations. A fifth class adds the
ready-data price-average comparison described below.

### `CsvFullRowProcessingBenchmark`

- streaming directly to the full-row consumer;
- `ArrayList` materialization with the expected row count as its initial capacity;
- `ArrayList` materialization starting with an initial capacity of 10;
- `LinkedList` materialization;
- `ProjectionStore` materialization with the expected row count as its initial capacity; and
- `ProjectionStore` materialization starting with an initial capacity of 10.

Each measured operation includes opening and reading the file, parsing CSV,
materializing the selected representation where applicable, and running the
same full-row checksum consumer. Dataset generation remains a separate,
unmeasured step.

### `CsvFilteredPriceSumEndToEndBenchmark`

Three end-to-end benchmarks produce the same aggregate from the same CSV input:

- `arrayListFilteredPriceSumEndToEnd` parses every `BenchmarkRow` into an
  expected-size `ArrayList`, then scans the list;
- `columnarFilteredPriceSumEndToEnd` parses every `BenchmarkRow` into an
  expected-size columnar `ProjectionStore`, then scans its quantity and price
  projections; and
- `reductionStoreFilteredPriceSumEndToEnd` feeds every `BenchmarkRow` to the
  generated reduction store, which incrementally applies `FilteredPriceSum`
  without retaining a row collection.

All three compute:

```text
quantity >= 5
sum(priceCents) for matching rows
```

Each measured operation includes opening and reading the file, parsing the same
`BenchmarkRow` objects through the same parser and `InputStreamProcessor`, and
producing the final `long` sum. The architectural work intentionally differs:
the `ArrayList` retains objects and performs a later traversal, the columnar
store retains projections and performs a later columnar traversal, and the
reduction store computes during ingestion without materializing retained rows.

### `ReadyPriceSumBenchmark` and `ReadyFilteredPriceSumBenchmark`

The ready-data benchmarks compare reductions after data has already been
materialized. `ReadyPriceSumBenchmark` contains the unfiltered selected-column
sum for `ArrayList`, `LinkedList`, and columnar `ProjectionStore`:

```text
sum(priceCents)
```

`ReadyFilteredPriceSumBenchmark` contains the filtered business operation for
the same three representations:

```text
quantity >= 5
sum(priceCents) for matching rows
```

The existing retained `LinkedList` scan benchmarks remain available as an
additional representation. These benchmarks prepare their structures in JMH
trial setup, so measured execution excludes CSV ingestion and materialization.
Separate JMH states retain only the representation required by a benchmark.
The retained `ArrayList` and `ProjectionStore` use the expected row count as
their initial capacity.

The initial-capacity comparison is intentionally limited to the end-to-end
benchmarks because it measures construction and growth cost. Once a structure is
already loaded, its starting capacity is not part of the measured scan.

### `ReadyPriceAverageBenchmark`

This benchmark asks: after the same deterministic collection of complete
`PriceTick(long timestamp, double price)` records has already been materialized,
how long does each representation's natural efficient public operation take to
calculate the average price?

Every comparator stores both timestamp and price even though the calculation
reads only price. `ArrayList<PriceTick>`, FastUtil
`ObjectArrayList<PriceTick>`, and Eclipse Collections `FastList<PriceTick>`
store records as objects. Tablesaw stores the same logical records column-wise
in a `Table` with timestamp and price columns, while Columnar Projection Store
stores both timestamp and price projections.

The measured operation selected for each representation is:

- `ArrayList`: indexed traversal with ordinary addition, divided by list size;
- FastUtil `ObjectArrayList`: traversal of the public `elements()` backing array
  through its logical size with ordinary addition, divided by list size;
- Eclipse Collections `FastList`: `sumOfDouble(PriceTick::price)`, divided by
  list size;
- Tablesaw: the price column's native `mean()` operation on the complete table; and
- Columnar Projection Store: price summation through its public cursor API,
  divided by store size.

Construction, deterministic data generation, row-count checks, and correctness
validation run in JMH trial setup and are excluded from measurement. Each JMH
state retains only its own representation. Numerical algorithms are allowed to
differ: notably, Eclipse Collections uses compensated summation. Results are
validated with a small floating-point tolerance instead of requiring
bit-identical output.

This ready-data suite repeatedly traverses each already-materialized
representation and reports its average execution time in microseconds per
operation. Each warmup and measurement iteration lasts one second; construction
and data generation remain outside the measured operation.

Without a `rowCount` override, `ReadyPriceAverageBenchmark` runs all five
representations at each of its four default sizes: 1,000, 100,000, 1,000,000,
and 10,000,000 rows. Passing `-p rowCount=...` overrides the source parameter
list for that invocation, so only the requested size is run.

Run only the five price-average methods with a chosen positive row count:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  ReadyPriceAverageBenchmark \
  -p rowCount=10000
```

This in-memory benchmark generates its deterministic records directly by row
index and does not read or generate a CSV dataset.

Run all methods with:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark'
```

Override the `rowCount` JMH parameter with `-p`; the corresponding dataset must
already exist for the CSV-backed benchmarks:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark' \
  -p rowCount=100000
```

Add JMH's GC profiler to collect allocation and garbage-collection metrics:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark' \
  -p rowCount=10000 \
  -prof gc
```

The GC profiler does not directly measure retained heap or peak heap usage.

Dataset generation is separate and unmeasured for all categories. JMH warmup
means filesystem and operating-system page-cache effects may be present in the
end-to-end comparisons. No performance conclusions should be drawn without
running controlled experiments on the intended hardware and dataset sizes.

## Run a benchmark manually on GitHub Actions

The `Manual Benchmarks` workflow is a manually dispatched, artifact-producing
alternative to the Bencher workflow. Choose one suite, one supported row count,
and one execution preset. Each workflow run maps the suite to exactly one
existing benchmark class, generates a CSV dataset only when that class requires
one, and uploads the JMH JSON results together with commit, input, command, Java,
Maven, operating-system, and CPU metadata. The in-memory
`ready-price-average` suite never uses a CSV dataset.

The presets control JMH execution as follows:

- `smoke`: 1 warmup iteration, 1 measurement iteration, and 1 fork;
- `default`: 2 warmup iterations, 3 measurement iterations, and 1 fork;
- `extended`: 5 warmup iterations, 10 measurement iterations, and 2 forks.

One workflow run executes one comparable benchmark class. Results from a
GitHub-hosted runner are suitable for comparing methods within that same
controlled run. Separate workflow runs may be scheduled on different hardware,
so their results should not be treated as directly comparable without accounting
for the recorded environment metadata.
