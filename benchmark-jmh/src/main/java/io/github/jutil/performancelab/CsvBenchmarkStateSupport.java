package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.github.jutil.columnarprojection.ProjectionStore;

/** Shared JMH state and dataset lookup support for CSV benchmarks. */
public final class CsvBenchmarkStateSupport {

    private CsvBenchmarkStateSupport() {}

    /** Retains only the ArrayList representation needed by ArrayList scan benchmarks. */
    @State(Scope.Benchmark)
    public static class ArrayListScanState {

        @Param({"10000"})
        public int rowCount;

        ArrayList<BenchmarkRow> rows;

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

        LinkedList<BenchmarkRow> rows;

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

        ProjectionStore<BenchmarkProjection> store;

        @Setup(Level.Trial)
        public void setup() throws IOException {
            Path csvFile = benchmarkCsvFile(rowCount);
            store = CsvPriceSumScans.loadColumnar(csvFile, rowCount);
        }
    }

    static Path benchmarkCsvFile(int rowCount) {
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
