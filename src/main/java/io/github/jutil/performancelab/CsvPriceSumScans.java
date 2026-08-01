package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;
import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.inputstreamprocessor.core.ProcessingResult;

/** Loading and selected-column scans for retained benchmark representations. */
final class CsvPriceSumScans {

    private static final InputStreamProcessor<BenchmarkRow> PROCESSOR =
            new InputStreamProcessor<>(new BenchmarkCsvParser());

    private CsvPriceSumScans() {
    }

    static List<BenchmarkRow> loadList(Path csvFile, int expectedRowCount) throws IOException {
        ArrayList<BenchmarkRow> rows = new ArrayList<>(expectedRowCount);
        try (InputStream input = Files.newInputStream(csvFile)) {
            BenchmarkCsvParser.parseRows(input, rows::add);
        }
        validateCount("list materialization", expectedRowCount, rows.size());
        return rows;
    }

    static ProjectionStore<BenchmarkProjection> loadColumnar(Path csvFile, int expectedRowCount)
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
        return store;
    }

    static long listPriceSum(List<BenchmarkRow> rows) {
        long sum = 0L;
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += rows.get(index).priceCents();
        }
        return sum;
    }

    static long columnarPriceSum(ProjectionStore<BenchmarkProjection> store) {
        long sum = 0L;
        ProjectionCursor<BenchmarkProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            sum += cursor.current().priceCents();
        }
        return sum;
    }

    private static void validateCount(String representation, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    representation + " loaded " + actual + " rows; expected " + expected);
        }
    }
}
