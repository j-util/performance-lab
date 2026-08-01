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

## Run benchmarks

After benchmarks are added, package and run the self-contained JMH runner:

```shell
./mvnw clean package
java -jar target/benchmarks.jar
```

No benchmark scenarios are included in the initial project structure.
