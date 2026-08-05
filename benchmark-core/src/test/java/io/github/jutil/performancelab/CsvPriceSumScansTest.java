package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;

class CsvPriceSumScansTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void retainedRepresentationsProduceStableIdenticalPriceAndFilteredSumsWithoutMutation()
            throws Exception {
        int rowCount = 17;
        Path dataset = temporaryDirectory.resolve("rows.csv");
        CsvDatasetGenerator.generate(rowCount, dataset);

        ArrayList<BenchmarkRow> arrayList = CsvPriceSumScans.loadArrayList(dataset, rowCount);
        LinkedList<BenchmarkRow> linkedList = CsvPriceSumScans.loadLinkedList(dataset, rowCount);
        ProjectionStore<BenchmarkProjection> store =
                CsvPriceSumScans.loadColumnar(dataset, rowCount);

        assertEquals(rowCount, arrayList.size());
        assertEquals(rowCount, linkedList.size());
        assertEquals(rowCount, store.size());

        List<BenchmarkRow> arrayListBeforeScan = List.copyOf(arrayList);
        List<BenchmarkRow> linkedListBeforeScan = List.copyOf(linkedList);
        List<BenchmarkRow> columnarBeforeScan = snapshot(store);

        long arrayListFirstSum = CsvPriceSumScans.arrayListPriceSum(arrayList);
        long arrayListSecondSum = CsvPriceSumScans.arrayListPriceSum(arrayList);
        long linkedListFirstSum = CsvPriceSumScans.linkedListPriceSum(linkedList);
        long linkedListSecondSum = CsvPriceSumScans.linkedListPriceSum(linkedList);
        long columnarFirstSum = CsvPriceSumScans.columnarPriceSum(store);
        long columnarSecondSum = CsvPriceSumScans.columnarPriceSum(store);

        assertEquals(arrayListFirstSum, linkedListFirstSum);
        assertEquals(arrayListFirstSum, columnarFirstSum);
        assertEquals(arrayListFirstSum, arrayListSecondSum);
        assertEquals(linkedListFirstSum, linkedListSecondSum);
        assertEquals(columnarFirstSum, columnarSecondSum);

        long arrayListFirstFilteredSum = CsvPriceSumScans.arrayListFilteredPriceSum(arrayList);
        long arrayListSecondFilteredSum = CsvPriceSumScans.arrayListFilteredPriceSum(arrayList);
        long linkedListFirstFilteredSum = CsvPriceSumScans.linkedListFilteredPriceSum(linkedList);
        long linkedListSecondFilteredSum = CsvPriceSumScans.linkedListFilteredPriceSum(linkedList);
        long columnarFirstFilteredSum = CsvPriceSumScans.columnarFilteredPriceSum(store);
        long columnarSecondFilteredSum = CsvPriceSumScans.columnarFilteredPriceSum(store);

        assertEquals(arrayListFirstFilteredSum, linkedListFirstFilteredSum);
        assertEquals(arrayListFirstFilteredSum, columnarFirstFilteredSum);
        assertEquals(arrayListFirstFilteredSum, arrayListSecondFilteredSum);
        assertEquals(linkedListFirstFilteredSum, linkedListSecondFilteredSum);
        assertEquals(columnarFirstFilteredSum, columnarSecondFilteredSum);

        assertEquals(arrayListBeforeScan, arrayList);
        assertEquals(linkedListBeforeScan, linkedList);
        assertEquals(columnarBeforeScan, snapshot(store));
        assertEquals(rowCount, arrayList.size());
        assertEquals(rowCount, linkedList.size());
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
