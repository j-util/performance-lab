package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

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

    static StrategyResult listMaterializationThenConsumer(Path csvFile, int expectedRowCount)
            throws IOException {
        ArrayList<BenchmarkRow> rows = new ArrayList<>(expectedRowCount);
        try (InputStream input = Files.newInputStream(csvFile)) {
            BenchmarkCsvParser.parseRows(input, rows::add);
        }
        validateCount("list materialization", expectedRowCount, rows.size());

        FullRowChecksumConsumer consumer = new FullRowChecksumConsumer();
        rows.forEach(consumer);
        validateCount("list checksum consumer", expectedRowCount, consumer.count());
        return new StrategyResult(consumer.checksum(), consumer.count());
    }

    static StrategyResult columnarMaterializationThenConsumer(Path csvFile, int expectedRowCount)
            throws IOException {
        ProjectionStore<BenchmarkProjection> store =
                ProjectionStores.create(BenchmarkProjection.class, expectedRowCount);
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
}
