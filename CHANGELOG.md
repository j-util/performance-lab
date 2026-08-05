# Changelog

All notable changes to this repository will be documented in this file.

## Unreleased

- Add an in-memory average-price benchmark across complete-record JDK,
  FastUtil, Eclipse Collections, Tablesaw, and Columnar Projection Store
  representations.
- Split the CSV benchmarks into four classes grouped by directly comparable operations.
- Restructure the benchmark project into shared core and JMH runner modules.
- Add the initial Maven, JMH, and JUnit 5 project structure.
- Add deterministic, streaming CSV dataset generation for future benchmarks.
