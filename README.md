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

## Run the full-row CSV benchmarks

The first CSV baseline compares streaming directly to a consumer, materializing
an `ArrayList` before `List.forEach`, and materializing a columnar projection
store before cursor traversal. Run all three methods with:

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

Each measured invocation includes opening and reading the file, parsing CSV,
creating `BenchmarkRow` objects, and processing or materializing rows according
to the selected strategy. Dataset generation is separate and unmeasured. JMH
warmup means filesystem and operating-system page-cache effects may be present.
No performance conclusions should be drawn without running controlled experiments
on the intended hardware and dataset sizes.
