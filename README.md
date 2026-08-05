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
- `benchmark-jmh` contains the OpenJDK JMH benchmark class and produces the
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

## Run the CSV benchmarks

The benchmark includes full-row processing comparisons and two separate
categories of reduction benchmarks.

### 1. End-to-end processing and materialization

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

### 2. Reduction over already-materialized data

The retained `ArrayList` and columnar `ProjectionStore` benchmarks compare
reductions after data has already been materialized. They include both an
unfiltered selected-column sum:

```text
sum(priceCents)
```

and the business operation:

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

### 3. End-to-end aggregate production

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

The initial-capacity comparison is intentionally limited to the end-to-end
benchmarks because it measures construction and growth cost. Once a structure is
already loaded, its starting capacity is not part of the measured scan.

Run all methods with:

```shell
java -jar benchmark-jmh/target/benchmarks.jar CsvFullRowBenchmark
```

Override the `rowCount` JMH parameter with `-p`; the corresponding dataset must
already exist:

```shell
java -jar benchmark-jmh/target/benchmarks.jar CsvFullRowBenchmark -p rowCount=100000
```

Add JMH's GC profiler to collect allocation and garbage-collection metrics:

```shell
java -jar benchmark-jmh/target/benchmarks.jar CsvFullRowBenchmark -p rowCount=10000 -prof gc
```

The GC profiler does not directly measure retained heap or peak heap usage.

Dataset generation is separate and unmeasured for all categories. JMH warmup
means filesystem and operating-system page-cache effects may be present in the
end-to-end comparisons. No performance conclusions should be drawn without
running controlled experiments on the intended hardware and dataset sizes.
