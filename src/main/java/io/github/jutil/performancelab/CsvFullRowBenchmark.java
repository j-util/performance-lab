package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.columnarprojection.ProjectionStore;

/** CSV benchmarks for full-row processing and retained selected-column scans. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class CsvFullRowBenchmark {

    @Param({"10000"})
    public int rowCount;

    private Path csvFile;

    @Setup
    public void setup() {
        csvFile = Path.of(
                "target", "benchmark-data", "benchmark-rows-" + rowCount + ".csv");
        if (!Files.isRegularFile(csvFile)) {
            throw new IllegalStateException(
                    "CSV benchmark dataset does not exist: " + csvFile.toAbsolutePath().normalize()
                            + ". Generate it first with CsvDatasetGenerator " + rowCount + ".");
        }
    }

    @Benchmark
    public long streamingToConsumer() throws IOException {
        return CsvProcessingStrategies.streamingToConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long listMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.listMaterializationThenConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long columnarMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.columnarMaterializationThenConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long listPriceSum(ListPriceSumState state) {
        return CsvPriceSumScans.listPriceSum(state.rows);
    }

    @Benchmark
    public long columnarPriceSum(ColumnarPriceSumState state) {
        return CsvPriceSumScans.columnarPriceSum(state.store);
    }

    /** Retains only the list representation used by {@link #listPriceSum(ListPriceSumState)}. */
    @State(Scope.Benchmark)
    public static class ListPriceSumState {

        @Param({"10000"})
        public int rowCount;

        private List<BenchmarkRow> rows;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            Path csvFile = benchmarkCsvFile(rowCount);
            rows = CsvPriceSumScans.loadList(csvFile, rowCount);
        }
    }

    /** Retains only the columnar representation used by {@link #columnarPriceSum(ColumnarPriceSumState)}. */
    @State(Scope.Benchmark)
    public static class ColumnarPriceSumState {

        @Param({"10000"})
        public int rowCount;

        private ProjectionStore<BenchmarkProjection> store;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            Path csvFile = benchmarkCsvFile(rowCount);
            store = CsvPriceSumScans.loadColumnar(csvFile, rowCount);
        }
    }

    private static Path benchmarkCsvFile(int rowCount) {
        Path csvFile = Path.of(
                "target", "benchmark-data", "benchmark-rows-" + rowCount + ".csv");
        if (!Files.isRegularFile(csvFile)) {
            throw new IllegalStateException(
                    "CSV benchmark dataset does not exist: " + csvFile.toAbsolutePath().normalize()
                            + ". Generate it first with CsvDatasetGenerator " + rowCount + ".");
        }
        return csvFile;
    }
}
