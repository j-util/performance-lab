# j-util Performance Lab

This repository contains reproducible benchmarks and performance experiments for
j-util libraries. It is a benchmark and demo workspace, not a reusable library.

Benchmark results depend on the hardware, operating system, JDK, JVM options, and
runtime conditions under which they are collected. Results from this repository
must not be treated as universal performance claims.

## Requirements

- JDK 17 or newer

The Maven Wrapper downloads the project's Maven version automatically.

## Build and test

```shell
./mvnw clean verify
```

Ordinary verification tests belong under `src/test/java` and use JUnit 5.

## Generate benchmark data

Dataset generation is a standalone step and is never performed as part of a
measured benchmark operation. Build the self-contained JAR, then pass the desired
row count to the generator:

```shell
./mvnw clean package
java -cp target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 1000000
```

This example deterministically writes 1,000,000 data rows plus a header to
`target/benchmark-data/benchmark-rows-1000000.csv`. Repeating the command with
the same row count produces identical CSV data. To choose another destination,
pass it as the second argument:

```shell
java -cp target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 1000 /tmp/benchmark.csv
```

Generated files under `target/` are build artifacts and must not be committed.

For a quick development run, generate the benchmark's default 10,000-row dataset:

```shell
java -cp target/benchmarks.jar io.github.jutil.performancelab.CsvDatasetGenerator 10000
```

## Run the CSV benchmarks

The benchmark answers three distinct questions.

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

### 2. Already-loaded selected-column reduction

The retained `ArrayList`, `LinkedList`, and `ProjectionStore` benchmarks each
compute:

```text
sum(priceCents)
```

### 3. Already-loaded primitive filter and reduction

The same three retained representations each apply the primitive filter and
reduction:

```text
quantity >= 5
sum(priceCents) for matching rows
```

Both already-loaded comparisons prepare their structures in JMH trial setup, so
measured execution excludes CSV ingestion and materialization. Separate JMH
states retain only the representation required by a benchmark. The retained
`ArrayList` and `ProjectionStore` use the expected row count as their initial
capacity.

The initial-capacity comparison is intentionally limited to the end-to-end
benchmarks because it measures construction and growth cost. Once a structure is
already loaded, its starting capacity is not part of the measured scan.

Run all methods with:

```shell
java -jar target/benchmarks.jar CsvFullRowBenchmark
```

Override the `rowCount` JMH parameter with `-p`; the corresponding dataset must
already exist:

```shell
java -jar target/benchmarks.jar CsvFullRowBenchmark -p rowCount=100000
```

Add JMH's GC profiler to collect allocation and garbage-collection metrics:

```shell
java -jar target/benchmarks.jar CsvFullRowBenchmark -p rowCount=10000 -prof gc
```

The GC profiler does not directly measure retained heap or peak heap usage.

Dataset generation is separate and unmeasured for all three categories. JMH warmup
means filesystem and operating-system page-cache effects may be present in the
end-to-end comparisons. No performance conclusions should be drawn without
running controlled experiments on the intended hardware and dataset sizes.
