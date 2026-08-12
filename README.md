# j-util Performance Lab

This repository contains reproducible benchmarks and performance experiments for
j-util libraries. It is a benchmark and demo workspace, not a reusable library.

Benchmark results depend on the hardware, operating system, JDK, JVM options, and
runtime conditions under which they are collected. Results from this repository
must not be treated as universal performance claims.

## Requirements

- JDK 25 or newer

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

### Generate the 1BRC-style processor dataset

The processor comparison uses a separate deterministic, headerless UTF-8
dataset with one ordinary Commons CSV record per line:

```text
station-name;temperature
```

Station names repeat realistically and temperatures always have one decimal
digit. The default is 10,000,000 measurements. Build with the configured Maven
settings and run the generator without arguments to use that default:

```shell
./mvnw -s /Users/karenbarseghyan/.m2/settings-j-util.xml clean package
java -cp benchmark-jmh/target/benchmarks.jar \
  io.github.jutil.performancelab.OneBrcStyleDatasetGenerator
```

Pass a row count to generate another scale. The conventional output is
`target/benchmark-data/1brc-style-measurements-<row-count>.csv`:

```shell
java -cp benchmark-jmh/target/benchmarks.jar \
  io.github.jutil.performancelab.OneBrcStyleDatasetGenerator 50000000
```

The generator reuses an existing file at that path when it has the requested
line count. Pass a second argument for an explicit output path. Generated data
stays under the ignored `target/` tree and must not be committed.

## Run the benchmarks

The benchmark methods are organized into classes so that each class contains
only directly comparable operations. Separate ready-data classes add the narrow
and wide average comparisons and the maximum-by-double comparison described
below, and a focused iteration suite compares traversal APIs within Columnar
Projection Store.

### `HardwoodMaterializationBenchmark`

This benchmark treats two independently written, valid Parquet files as one
deterministic flat market-data input. The first file contains `rowCount / 4`
rows and the second contains the remainder, with one shared schema and globally
consecutive values across the file boundary. Both paths pass the files in the
same order to one real Hardwood multi-file reader and use the same projection:

- `hardwoodToColumnarBatch` calls the generated
  `HardwoodMarketDataProjectionHardwoodLoader.load(reader)` convenience method.
  The loader creates the projected column readers, uses the generated
  common-range batch API, materializes every file, and seals the store.
- `hardwoodToArrayList` creates one immutable `HardwoodMarketDataRow` record per
  decoded row and appends it to an `ArrayList`, representing conventional object
  materialization.

Both paths inspect every file through
`reader.getFileMetaData(fileIndex).numRows()`, combine the row counts with
`Math.addExact`, and convert the result to an exact initial capacity with
`Math.toIntExact`. The `ArrayList` and the store created by the generated
columnar loader therefore both start with the exact combined row capacity for
the two-file input.

Every invocation opens a fresh
`ParquetFileReader.openAll(List.of(InputFile.of(firstPath),
InputFile.of(secondPath)))`, lets Hardwood transition between the files, and
closes its projected column readers and multi-file reader before returning the
completed destination to JMH. The timed boundary includes opening the files,
indexed metadata access for exact initial sizing, Hardwood column-reader
construction, Parquet decoding across both files, the file transition,
destination materialization, columnar sealing, and resource closing. Trial
setup generates the two-file dataset outside benchmark timing, and trial
teardown deletes it. Correctness checks, result traversal, checksums, and
aggregations also remain outside measured code.

This is a realistic end-to-end destination-materialization comparison, not an
ingestion-only benchmark over retained decoded arrays. It does not measure any
subsequent column operation, query, checksum, aggregation, or validation scan.
Because Hardwood decoding is shared work, it can reduce the visible timing
difference between the destinations. GC-profiler allocation measurements include
Hardwood decoding and destination materialization; the columnar path avoids the
one row object per record created by the `ArrayList` path. The returned
destinations are JMH results, preventing dead-code
elimination. The fixture uses uncompressed Parquet with dictionary encoding
disabled and deterministic low-cardinality strings and exactly representable
numeric values. Results from the earlier single-file, combined-capacity benchmark
are not directly comparable with this multi-file workload.

Development note: this configuration requires the locally installed
`1.1.0-SNAPSHOT` Hardwood Core artifact and Columnar Projection Store Hardwood
runtime and annotation-processor artifacts in `~/.m2`. It will not resolve on
ordinary CI until Hardwood 1.1.0 is published or the dependency becomes
otherwise available.

Run the ordinary small correctness test, which checks the unequal two-file
fixture, Hardwood's multi-file state, both indexed file-metadata entries, the
cross-file boundary and second-file consumption, row counts, global row order,
every field, batch boundaries, a partial final batch, exact combined store
capacity and sealing, independent invocations, stable string references, and
representation equivalence:

```shell
./mvnw -pl benchmark-core \
  -Dtest=HardwoodMaterializationCasesTest test
```

Build and run a short 10,000-row smoke invocation of both methods. Smoke-test
output only confirms that the benchmark executes; it is not a stable benchmark
result:

```shell
./mvnw clean package
java -jar benchmark-jmh/target/benchmarks.jar \
  '.*HardwoodMaterializationBenchmark.*' \
  -p rowCount=10000 \
  -wi 1 \
  -i 1 \
  -f 1 \
  -prof gc
```

Run a representative 1,000,000-row measurement with JMH GC allocation metrics:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  HardwoodMaterializationBenchmark \
  -p rowCount=1000000 \
  -wi 2 \
  -i 3 \
  -f 1 \
  -prof gc
```

The default `rowCount` parameters are 1,000,000 and 10,000,000. Because the
same generated files are read for every invocation within a trial, measurements
after the first read generally benefit from the operating system's page cache.

### `OneBrcStyleProcessorBenchmark`

This is a fair 1BRC-style processor benchmark, not an optimized or official
1BRC submission. All five variants parse the same two fields with the same
immutable Apache Commons CSV format (semicolon delimiter, no header, no
trimming or surrounding-space assumptions) and map each record to
`Item(String key, double value)` using `Double.parseDouble`:

| Benchmark | Execution strategy | Parallelism |
| --- | --- | ---: |
| `filesLinesSequential` | sequential `Files.lines()` | 1 |
| `filesLinesParallelForkJoinPool` | `Files.lines().parallel()` in a caller-created `ForkJoinPool` | 2, 4, 8 |
| `inputStreamProcessorCore` | sequential `inputstream-processor-core` | 1 |
| `parallelRangeProcessorForkJoinPool` | `parallel-range-processor` with a caller-created `ForkJoinPool` | 1, 2, 4, 8 |
| `parallelRangeProcessorFixedThreadPool` | `parallel-range-processor` with a caller-created fixed-thread-pool `ExecutorService` | 1, 2, 4, 8 |

Every aggregation uses `Storage` with a `HashMap<String, Counter>`. Each
`Counter` maintains minimum, maximum, sum, and count and calculates mean only as
`sum / count`. Parallel stream partitions and range-processor parsers create
local `Storage` instances; their partial results merge only after local work,
without a shared `ConcurrentHashMap`.

The range-processor states construct the processor and their caller-owned
executor once in JMH trial setup. The processor receives that executor through
its public constructor, never owns it, and never shuts it down. Trial teardown
shuts down and awaits the `ForkJoinPool` or fixed thread pool. The parallel
`Files.lines()` state likewise creates one dedicated reusable `ForkJoinPool`, so
it never relies on the common pool. The fixed thread pool is an additional
caller-supplied executor comparison for `parallel-range-processor`; it is not a
library default or an internally owned pool.

Measured execution includes file opening, processing, parsing, aggregation,
and partial-result merging. Dataset generation, executor construction,
processor construction, and correctness assertions are outside measurement.
Each invocation starts with fresh aggregation storage, and no benchmark method
prints results.

Commons CSV parser lifecycle necessarily follows each API's natural boundary.
`inputstream-processor-core` uses one parser for the complete input,
`parallel-range-processor` creates one independent parser per actual range and
another for reconstructed boundary records when needed, and each already-framed
line from `Files.lines()` is parsed by its own Commons CSV parser. Thus parsing
logic and format are shared, while parser-instance counts are an unavoidable
fairness difference. The range variants also include byte-range framing and
partial aggregation merging; the stream variants include the JDK stream's own
splitting and line-decoding behavior.

For a short smoke run after generating 1,000 rows:

```shell
java -cp benchmark-jmh/target/benchmarks.jar \
  io.github.jutil.performancelab.OneBrcStyleDatasetGenerator 1000
java -jar benchmark-jmh/target/benchmarks.jar \
  OneBrcStyleProcessorBenchmark \
  -p rowCount=1000 \
  -wi 1 \
  -i 1 \
  -f 1
```

Run the full default 10,000,000-row comparison with:

```shell
java -cp benchmark-jmh/target/benchmarks.jar \
  io.github.jutil.performancelab.OneBrcStyleDatasetGenerator 10000000
java -jar benchmark-jmh/target/benchmarks.jar \
  OneBrcStyleProcessorBenchmark \
  -p rowCount=10000000 \
  -wi 5 \
  -i 10 \
  -f 2
```

Run the 50,000,000-row scaling comparison without code changes with:

```shell
java -cp benchmark-jmh/target/benchmarks.jar \
  io.github.jutil.performancelab.OneBrcStyleDatasetGenerator 50000000
java -jar benchmark-jmh/target/benchmarks.jar \
  OneBrcStyleProcessorBenchmark \
  -p rowCount=50000000 \
  -wi 5 \
  -i 10 \
  -f 2
```

Because the same file is read repeatedly across warmup and measurement
iterations, results primarily represent warm OS-page-cache processing rather
than raw disk throughput.

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

This benchmark asks: after deterministic input has already been materialized,
how long do native convenient operations and identical naive arithmetic take to
calculate the average price across the compared storage representations?

All actual collection and store comparators retain the complete logical
`PriceTick(long timestamp, double price)` record even though the calculation
reads only price. `ArrayList<PriceTick>`, FastUtil
`ObjectArrayList<PriceTick>`, and Eclipse Collections `FastList<PriceTick>`
store records as objects. Tablesaw stores the same logical records column-wise
in a `Table` with timestamp and price columns, while Columnar Projection Store
stores both timestamp and price projections.

The ordinary-addition comparator methods are:

- `ArrayList`: indexed traversal with ordinary addition, divided by list size;
- FastUtil `ObjectArrayList`: traversal of the public `elements()` backing array
  through its logical size with ordinary addition, divided by list size;
- Eclipse Collections `FastList` naive: explicit indexed traversal of the
  existing list with ordinary addition, divided by list size;
- Tablesaw naive: explicit indexed traversal of the existing price
  `DoubleColumn` with ordinary addition, divided by column size; and
- Columnar Projection Store: price summation through its public cursor API,
  divided by store size.

The native-operation methods coexist with those identical-naive-arithmetic
comparisons:

- Eclipse Collections `FastList`: native `sumOfDouble(PriceTick::price)`,
  divided by list size; and
- Tablesaw: the price column's native `mean()` operation on the complete table.

The separate `double[]` calculation baseline contains only prices. It traverses
that array directly with ordinary addition and divides by the array length,
providing the lowest-abstraction reference for the measured calculation. It is
intentionally not a complete `PriceTick` representation. This documented
exception must not be used for full-record retained-memory-footprint claims.

Construction, deterministic data generation, row-count checks, and correctness
validation run in JMH trial setup and are excluded from measurement. Each JMH
state retains only its own representation, and the native and naive methods for
each framework reuse the same state. Ordinary-addition implementations traverse
the same finite prices in the same encounter order and are checked for exact
agreement. Eclipse Collections and Tablesaw retain their existing native
operation semantics and are validated with a small floating-point tolerance
because their arithmetic order can differ.

This ready-data suite repeatedly traverses each already-materialized
representation and reports its average execution time in microseconds per
operation. Each warmup and measurement iteration lasts one second; construction
and data generation remain outside the measured operation.

Without a `rowCount` override, `ReadyPriceAverageBenchmark` runs all eight
methods at each of its four default sizes: 1,000, 100,000, 1,000,000, and
10,000,000 rows. Passing `-p rowCount=...` overrides the source parameter list
for that invocation, so only the requested size is run.

Run only the eight price-average methods with a chosen positive row count:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  ReadyPriceAverageBenchmark \
  -p rowCount=10000
```

This in-memory benchmark generates its deterministic records directly by row
index and does not read or generate a CSV dataset.

### `MaxByDoubleBenchmark`

This serial ready-data suite is an
[Eclipse Collections `MaxByDoubleTest`](https://github.com/eclipse-collections/eclipse-collections/blob/master/jmh-tests/src/main/java/org/eclipse/collections/impl/jmh/MaxByDoubleTest.java)
JMH-derived workload. It uses project-owned deterministic domain and fixture
code; it is not an official Eclipse Collections benchmark result.

Every method performs the same logical operation: scan all `Position` rows by
the primitive `double marketValue`, calculated as `quantity * product.price()`,
and return the original `Position` having the maximum value. A deterministic
shared product population and row order are used. The fixture places one unique
maximum at a non-terminal position for datasets of at least three rows,
avoiding tie-semantics differences between implementations.

The five benchmark methods are:

- `arrayListImperativeMaxByDouble`: indexed imperative traversal of a JDK
  `ArrayList<Position>`;
- `arrayListStreamMaxByDouble`: a serial JDK stream using
  `Comparator.comparingDouble`;
- `eclipseFastListMaxByDouble`: Eclipse Collections `FastList.maxBy`;
- `columnarProjectionStoreMaxByDouble`: one Columnar Projection Store cursor
  over projected market values and retained original references; and
- `manualHybridMaxByDouble`: a manual lower-bound baseline pairing a complete
  `Position[]` reference array with a precomputed `double[]` market-value array.

This is intentionally a repeated-query comparison after construction.
Generation, allocation, representation population, projection evaluation,
sealing, and correctness validation occur in JMH trial setup and are excluded
from the measured scan. Columnar Projection Store and the manual hybrid compute
and retain `marketValue` during population. The object collections instead call
`Position.marketValue()` during every measured scan. This asymmetry is part of
the ready-data comparison and means the suite does not measure total end-to-end
cost.

The only default `rowCount` is 3,000,000, matching the scale of the source
workload. A command-line `-p rowCount=...` overrides that value. Run a quick
smoke test with:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  MaxByDoubleBenchmark \
  -p rowCount=1000 \
  -wi 1 \
  -i 1 \
  -f 1
```

For a publication-quality run at the default scale:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  MaxByDoubleBenchmark \
  -p rowCount=3000000 \
  -wi 5 \
  -i 10 \
  -f 2
```

Results are not directly comparable with official Eclipse Collections runs:
this suite has a different implementation set, deterministic project-owned
fixtures, and different harness modes and configuration. No parallel variants
are included because they would add a separate execution-model comparison.

### `ReadyMarketDataSnapshotAverageBenchmark`

This separate wide-record suite performs the same ready-data average operation
over a realistic `MarketDataSnapshot` containing capture time, symbol, last
trade price and size, best bid and ask prices, and best bid and ask sizes. Each
fixture row is the state immediately after a distinct completed trade, and the
measured operation reads only `lastTradePrice`.

Unlike the narrow `PriceTick(timestamp, price)` suite, every complete comparator
in this suite retains all eight snapshot fields. The storage representations fall
into four categories:

- heap row objects: `ArrayList<MarketDataSnapshot>`, FastUtil
  `ObjectArrayList<MarketDataSnapshot>`, and Eclipse Collections
  `FastList<MarketDataSnapshot>`;
- off-heap row records: a raw JDK `MemorySegment` baseline with a fixed-width
  64-byte row layout, and typed Chronicle Values flyweights over consecutive
  direct Chronicle Bytes records;
- on-heap columnar storage: Tablesaw `Table`, a DFLib `DataFrame`, Columnar
  Projection Store, and complete manually assembled HPPC primitive columns; and
- off-heap columnar storage: an Apache Arrow `VectorSchemaRoot` containing one
  vector for each snapshot field.

The MemorySegment representation is the raw off-heap row baseline. Its symbol
field stores a one-byte UTF-8 length followed by up to seven UTF-8 bytes, and it
uses explicit `Arena` ownership. Chronicle Values + Bytes provides the typed
off-heap row representation with the same seven-byte UTF-8 symbol capacity and
one reusable flyweight. Apache Arrow is the established off-heap columnar
representation, using `BigIntVector` for capture time, `VarCharVector` for the
symbol, and `Float8Vector` for all six double fields. All three retain the
complete eight-field snapshot and release native resources at JMH trial teardown.
The representations therefore compare the same complete logical records even
though the calculation selects a single field.

The DFLib comparator is one complete in-memory columnar `DataFrame` containing
one long series, one String series, and six double series. Its native method
delegates average calculation to DFLib's public `DoubleSeries.avg()` operation,
while its naive method indexes that same retained `lastTradePrice` series and
performs ordinary encounter-order addition. The HPPC comparator is not a
DataFrame: it is a complete manual column layout made from one `LongArrayList`,
one `ObjectArrayList<String>`, and six `DoubleArrayList` instances. Its measured
method indexes only the retained `lastTradePrice` list with ordinary addition.
Both representations retain all eight snapshot fields, while the measured
operation reads only `lastTradePrice`.

As in the narrow suite, the separate `double[]` baseline contains only the
generated `lastTradePrice` values. It is a calculation-only reference and must
not be included in complete-record retained-memory comparisons. Eclipse
Collections' native `sumOfDouble()`, Tablesaw's native `mean()`, and DFLib's
native `avg()` methods retain their framework-defined numerical semantics; the
naive companion methods define ordinary encounter-order addition explicitly.
Native results are therefore validated with the existing small floating-point
tolerance instead of making their arithmetic implementation part of this
benchmark's contract.

The suite measures hot sequential traversal of `lastTradePrice`; every ordinary
implementation adds doubles in encounter order and divides by its logical row
count. Setup, allocation, fixture generation, population, and validation are
excluded from the measured operation. Off-heap storage primarily provides an
explicit lifecycle and reduced heap and garbage-collection pressure; it is not
assumed to be faster, and no performance claim is made before results are
collected.

The suite uses the same JMH configuration and default row counts as
`ReadyPriceAverageBenchmark`. Run all fourteen wide-record methods at one positive
row count with:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  ReadyMarketDataSnapshotAverageBenchmark \
  -p rowCount=10000
```

Construction, fixture generation, and validation remain outside measured time.
No comparative performance or retained-memory conclusion is claimed before
controlled results are collected on the intended hardware and row counts.

Chronicle Values performs runtime value-class generation and Apache Arrow's
Netty allocator accesses direct-buffer internals. Maven tests configure the
required module access. The packaged JMH methods also append the narrowly scoped
fork arguments automatically: Chronicle opens `java.lang`, exports
`jdk.compiler/com.sun.tools.javac.file`, and enables native access; Arrow opens
`java.nio`. Both native-library forks allow the legacy `sun.misc.Unsafe` memory
operations required when running on JDK 26. Users do not need to discover or add
these flags when running the packaged benchmark normally.

### `ColumnarProjectionStoreIterationBenchmark`

This focused ready-data suite compares the ergonomics and efficiency of the
three public row-oriented traversal APIs on the same sealed Columnar Projection
Store. The cursor exposes one reusable projection view whose contents advance
with the cursor and therefore must not be retained. Indexed `viewAt(index)`
provides explicit random access through stable, retainable views. `forEach` is
the conventional OO traversal API and also supplies stable, retainable views.

The benchmark measures the cost of the stable-view convenience; it does not
assume in advance that one traversal will be faster or allocate more at runtime.
JIT escape analysis may eliminate some temporary stable-view allocations. Run
with JMH's GC profiler to observe the allocation behavior that remains in the
measured runtime:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  ColumnarProjectionStoreIterationBenchmark \
  -p rowCount=1000000 \
  -prof gc
```

Each API runs both a narrow `lastTradePrice` sum and a checksum that reads all
eight fields. Within a workload, all traversal mechanisms pass every row to the
same resettable accumulator, visit rows in encounter order, and reuse the
Consumer between invocations. Store construction, fixture generation, sealing,
and validation happen in trial setup rather than measured code. The cursor
itself is created inside each cursor operation, matching normal public usage.

For publication-quality measurements, increase warmup, measurement, and fork
counts and use the largest configured data set:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  ColumnarProjectionStoreIterationBenchmark \
  -p rowCount=10000000 \
  -wi 5 \
  -i 10 \
  -f 2 \
  -prof gc
```

These APIs deliberately have different contracts: the cursor prioritizes
maximum traversal efficiency through a reusable view, `viewAt` provides stable
views for explicit random access, and `forEach` provides conventional OO
traversal through stable views. Interpret timing and allocation results in that
ergonomics-versus-efficiency context rather than treating convenience as an
inferior contract.

Run all methods with:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark|MaxByDoubleBenchmark|ReadyMarketDataSnapshotAverageBenchmark|ColumnarProjectionStoreIterationBenchmark'
```

Override the `rowCount` JMH parameter with `-p`; the corresponding dataset must
already exist for the CSV-backed benchmarks:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark|MaxByDoubleBenchmark|ReadyMarketDataSnapshotAverageBenchmark|ColumnarProjectionStoreIterationBenchmark' \
  -p rowCount=100000
```

Add JMH's GC profiler to collect allocation and garbage-collection metrics:

```shell
java -jar benchmark-jmh/target/benchmarks.jar \
  'CsvFullRowProcessingBenchmark|CsvFilteredPriceSumEndToEndBenchmark|ReadyPriceSumBenchmark|ReadyFilteredPriceSumBenchmark|ReadyPriceAverageBenchmark|MaxByDoubleBenchmark|ReadyMarketDataSnapshotAverageBenchmark|ColumnarProjectionStoreIterationBenchmark' \
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
`ready-price-average` and `max-by-double` suites never use a CSV dataset.

The presets control JMH execution as follows:

- `smoke`: 1 warmup iteration, 1 measurement iteration, and 1 fork;
- `default`: 2 warmup iterations, 3 measurement iterations, and 1 fork;
- `extended`: 5 warmup iterations, 10 measurement iterations, and 2 forks.

One workflow run executes one comparable benchmark class. Results from a
GitHub-hosted runner are suitable for comparing methods within that same
controlled run. Separate workflow runs may be scheduled on different hardware,
so their results should not be treated as directly comparable without accounting
for the recorded environment metadata.
