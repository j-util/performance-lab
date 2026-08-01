package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
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
    public long arrayListExpectedSizeMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .arrayListExpectedSizeMaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long arrayListInitial10MaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .arrayListInitial10MaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long linkedListMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.linkedListMaterializationThenConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long columnarExpectedSizeMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .columnarExpectedSizeMaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long columnarInitial10MaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .columnarInitial10MaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long arrayListPriceSum(ArrayListScanState state) {
        return CsvPriceSumScans.arrayListPriceSum(state.rows);
    }

    @Benchmark
    public long linkedListPriceSum(LinkedListScanState state) {
        return CsvPriceSumScans.linkedListPriceSum(state.rows);
    }

    @Benchmark
    public long columnarPriceSum(ColumnarScanState state) {
        return CsvPriceSumScans.columnarPriceSum(state.store);
    }

    @Benchmark
    public long arrayListFilteredPriceSum(ArrayListScanState state) {
        return CsvPriceSumScans.arrayListFilteredPriceSum(state.rows);
    }

    @Benchmark
    public long linkedListFilteredPriceSum(LinkedListScanState state) {
        return CsvPriceSumScans.linkedListFilteredPriceSum(state.rows);
    }

    @Benchmark
    public long columnarFilteredPriceSum(ColumnarScanState state) {
        return CsvPriceSumScans.columnarFilteredPriceSum(state.store);
    }

    /** Retains only the ArrayList representation needed by ArrayList scan benchmarks. */
    @State(Scope.Benchmark)
    public static class ArrayListScanState {

        @Param({"10000"})
        public int rowCount;

        private ArrayList<BenchmarkRow> rows;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            Path csvFile = benchmarkCsvFile(rowCount);
            rows = CsvPriceSumScans.loadArrayList(csvFile, rowCount);
        }
    }

    /** Retains only the LinkedList representation needed by LinkedList scan benchmarks. */
    @State(Scope.Benchmark)
    public static class LinkedListScanState {

        @Param({"10000"})
        public int rowCount;

        private LinkedList<BenchmarkRow> rows;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            Path csvFile = benchmarkCsvFile(rowCount);
            rows = CsvPriceSumScans.loadLinkedList(csvFile, rowCount);
        }
    }

    /** Retains only the ProjectionStore representation needed by columnar scan benchmarks. */
    @State(Scope.Benchmark)
    public static class ColumnarScanState {

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
