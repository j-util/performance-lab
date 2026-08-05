package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;
import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.inputstreamprocessor.core.ProcessingResult;

/** End-to-end processing paths exercised by the CSV benchmark and its tests. */
final class CsvProcessingStrategies {

    private static final InputStreamProcessor<BenchmarkRow> PROCESSOR =
            new InputStreamProcessor<>(new BenchmarkCsvParser());

    private CsvProcessingStrategies() {
    }

    static StrategyResult streamingToConsumer(Path csvFile, int expectedRowCount) throws IOException {
        FullRowChecksumConsumer consumer = new FullRowChecksumConsumer();
        ProcessingResult processingResult;
        try (InputStream input = Files.newInputStream(csvFile)) {
            processingResult = PROCESSOR.process(input, consumer);
        }
        validateCount("streaming", expectedRowCount, processingResult.getProcessedCount());
        validateCount("streaming checksum consumer", expectedRowCount, consumer.count());
        return new StrategyResult(consumer.checksum(), consumer.count());
    }

    static FilteredPriceSumResult arrayListFilteredPriceSumEndToEnd(
            Path csvFile, int expectedRowCount) throws IOException {
        ArrayList<BenchmarkRow> rows = new ArrayList<>(expectedRowCount);
        ProcessingResult processingResult;
        try (InputStream input = Files.newInputStream(csvFile)) {
            processingResult = PROCESSOR.process(input, rows::add);
        }
        validateCount(
                "ArrayList filtered-price input processing",
                expectedRowCount,
                processingResult.getProcessedCount());
        validateCount("ArrayList filtered-price materialization", expectedRowCount, rows.size());
        return new FilteredPriceSumResult(
                CsvPriceSumScans.arrayListFilteredPriceSum(rows),
                processingResult.getProcessedCount());
    }

    static FilteredPriceSumResult columnarFilteredPriceSumEndToEnd(
            Path csvFile, int expectedRowCount) throws IOException {
        ProjectionStore<BenchmarkProjection> store =
                ProjectionStores.create(BenchmarkProjection.class, expectedRowCount);
        ProcessingResult processingResult;
        try (InputStream input = Files.newInputStream(csvFile)) {
            processingResult = PROCESSOR.process(input, store::add);
        }
        validateCount(
                "columnar filtered-price input processing",
                expectedRowCount,
                processingResult.getProcessedCount());
        validateCount("columnar filtered-price materialization", expectedRowCount, store.size());
        store.seal();
        return new FilteredPriceSumResult(
                CsvPriceSumScans.columnarFilteredPriceSum(store),
                processingResult.getProcessedCount());
    }

    static FilteredPriceSumResult reductionStoreFilteredPriceSumEndToEnd(
            Path csvFile, int expectedRowCount) throws IOException {
        BenchmarkRowReductionStore store = new BenchmarkRowReductionStore();
        ProcessingResult processingResult;
        try (InputStream input = Files.newInputStream(csvFile)) {
            processingResult = PROCESSOR.process(input, store::add);
        }
        validateCount(
                "reduction-store filtered-price input processing",
                expectedRowCount,
                processingResult.getProcessedCount());
        return new FilteredPriceSumResult(
                store.filteredPriceSum(), processingResult.getProcessedCount());
    }

    static StrategyResult arrayListExpectedSizeMaterializationThenConsumer(
            Path csvFile, int expectedRowCount)
            throws IOException {
        return listMaterializationThenConsumer(
                csvFile, expectedRowCount, new ArrayList<>(expectedRowCount), "ArrayList expected-size");
    }

    static StrategyResult arrayListInitial10MaterializationThenConsumer(
            Path csvFile, int expectedRowCount)
            throws IOException {
        return listMaterializationThenConsumer(
                csvFile, expectedRowCount, new ArrayList<>(10), "ArrayList initial-10");
    }

    static StrategyResult linkedListMaterializationThenConsumer(Path csvFile, int expectedRowCount)
            throws IOException {
        return listMaterializationThenConsumer(
                csvFile, expectedRowCount, new LinkedList<>(), "LinkedList");
    }

    static StrategyResult columnarExpectedSizeMaterializationThenConsumer(
            Path csvFile, int expectedRowCount)
            throws IOException {
        return columnarMaterializationThenConsumer(csvFile, expectedRowCount, expectedRowCount);
    }

    static StrategyResult columnarInitial10MaterializationThenConsumer(
            Path csvFile, int expectedRowCount)
            throws IOException {
        return columnarMaterializationThenConsumer(csvFile, expectedRowCount, 10);
    }

    private static StrategyResult listMaterializationThenConsumer(
            Path csvFile, int expectedRowCount, List<BenchmarkRow> rows, String strategy)
            throws IOException {
        try (InputStream input = Files.newInputStream(csvFile)) {
            BenchmarkCsvParser.parseRows(input, rows::add);
        }
        validateCount(strategy + " materialization", expectedRowCount, rows.size());

        FullRowChecksumConsumer consumer = new FullRowChecksumConsumer();
        rows.forEach(consumer);
        validateCount(strategy + " checksum consumer", expectedRowCount, consumer.count());
        return new StrategyResult(consumer.checksum(), consumer.count());
    }

    private static StrategyResult columnarMaterializationThenConsumer(
            Path csvFile, int expectedRowCount, int initialCapacity)
            throws IOException {
        ProjectionStore<BenchmarkProjection> store =
                ProjectionStores.create(BenchmarkProjection.class, initialCapacity);
        ProcessingResult processingResult;
        try (InputStream input = Files.newInputStream(csvFile)) {
            processingResult = PROCESSOR.process(input, store::add);
        }
        validateCount("columnar input processing", expectedRowCount, processingResult.getProcessedCount());
        validateCount("columnar materialization", expectedRowCount, store.size());

        store.seal();
        ProjectionCursor<BenchmarkProjection> cursor = store.cursor();
        FullRowChecksumConsumer consumer = new FullRowChecksumConsumer();
        while (cursor.moveNext()) {
            consumer.accept(cursor.current());
        }
        validateCount("columnar checksum consumer", expectedRowCount, consumer.count());
        return new StrategyResult(consumer.checksum(), consumer.count());
    }

    private static void validateCount(String strategy, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    strategy + " processed " + actual + " rows; expected " + expected);
        }
    }

    record StrategyResult(long checksum, long rowCount) {
    }

    record FilteredPriceSumResult(long sum, long rowCount) {
    }
}
