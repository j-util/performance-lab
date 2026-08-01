package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;

class CsvPriceSumScansTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void retainedRepresentationsProduceStableIdenticalPriceSumsWithoutMutation() throws Exception {
        int rowCount = 17;
        Path dataset = temporaryDirectory.resolve("rows.csv");
        CsvDatasetGenerator.generate(rowCount, dataset);

        List<BenchmarkRow> rows = CsvPriceSumScans.loadList(dataset, rowCount);
        ProjectionStore<BenchmarkProjection> store =
                CsvPriceSumScans.loadColumnar(dataset, rowCount);

        assertEquals(rowCount, rows.size());
        assertEquals(rowCount, store.size());

        List<BenchmarkRow> listBeforeScan = List.copyOf(rows);
        List<BenchmarkRow> columnarBeforeScan = snapshot(store);

        long listFirstSum = CsvPriceSumScans.listPriceSum(rows);
        long listSecondSum = CsvPriceSumScans.listPriceSum(rows);
        long columnarFirstSum = CsvPriceSumScans.columnarPriceSum(store);
        long columnarSecondSum = CsvPriceSumScans.columnarPriceSum(store);

        assertEquals(listFirstSum, columnarFirstSum);
        assertEquals(listFirstSum, listSecondSum);
        assertEquals(columnarFirstSum, columnarSecondSum);
        assertEquals(listBeforeScan, rows);
        assertEquals(columnarBeforeScan, snapshot(store));
        assertEquals(rowCount, rows.size());
        assertEquals(rowCount, store.size());
    }

    private static List<BenchmarkRow> snapshot(ProjectionStore<BenchmarkProjection> store) {
        List<BenchmarkRow> rows = new ArrayList<>(store.size());
        ProjectionCursor<BenchmarkProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            BenchmarkProjection row = cursor.current();
            rows.add(new BenchmarkRow(
                    row.id(),
                    row.customerId(),
                    row.productId(),
                    row.quantity(),
                    row.priceCents(),
                    row.timestamp(),
                    row.region(),
                    row.status()));
        }
        return rows;
    }
}
