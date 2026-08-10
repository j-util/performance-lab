package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.columnarprojection.ProjectionStore;

class HardwoodMaterializationCasesTest {

    private static final int BATCH_SIZE = 7;
    private static final int ROW_COUNT = BATCH_SIZE * 2 + 3;

    @TempDir
    Path temporaryDirectory;

    @Test
    void bothPathsMaterializeEquivalentOrderedRowsAcrossBatchBoundaries() throws Exception {
        Path parquetFile = temporaryDirectory.resolve("market-data.parquet");
        HardwoodParquetDatasetGenerator.write(parquetFile, ROW_COUNT);

        ProjectionStore<HardwoodMarketDataProjection> store =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ArrayList<HardwoodMarketDataRow> rows =
                HardwoodMaterializationCases.hardwoodToArrayList(
                        parquetFile, ROW_COUNT, BATCH_SIZE);

        assertEquals(ROW_COUNT, store.size());
        assertEquals(ROW_COUNT, rows.size());
        assertSealed(store);

        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            assertProjectionEquals(expected, store.viewAt(rowIndex), "columnar row " + rowIndex);
            assertEquals(expected, rows.get(rowIndex), "ArrayList row " + rowIndex);
            assertProjectionEquals(rows.get(rowIndex), store.viewAt(rowIndex),
                    "destination equivalence at row " + rowIndex);
        }

        assertSelectedRows(store, rows, 0, BATCH_SIZE - 1, BATCH_SIZE,
                ROW_COUNT / 2, BATCH_SIZE * 2 - 1, BATCH_SIZE * 2, ROW_COUNT - 1);
        assertStableStringReferences(store, rows, BATCH_SIZE);
    }

    @Test
    void repeatedInvocationsCreateIndependentDestinationsPositionedAtTheBeginning()
            throws Exception {
        Path parquetFile = temporaryDirectory.resolve("repeated-market-data.parquet");
        HardwoodParquetDatasetGenerator.write(parquetFile, ROW_COUNT);

        ProjectionStore<HardwoodMarketDataProjection> firstStore =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ProjectionStore<HardwoodMarketDataProjection> secondStore =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ArrayList<HardwoodMarketDataRow> firstRows =
                HardwoodMaterializationCases.hardwoodToArrayList(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ArrayList<HardwoodMarketDataRow> secondRows =
                HardwoodMaterializationCases.hardwoodToArrayList(
                        parquetFile, ROW_COUNT, BATCH_SIZE);

        assertNotSame(firstStore, secondStore);
        assertNotSame(firstRows, secondRows);
        assertProjectionEquals(HardwoodParquetDatasetGenerator.rowAt(0), firstStore.viewAt(0),
                "first columnar invocation");
        assertProjectionEquals(HardwoodParquetDatasetGenerator.rowAt(0), secondStore.viewAt(0),
                "second columnar invocation");
        assertEquals(HardwoodParquetDatasetGenerator.rowAt(0), firstRows.get(0));
        assertEquals(HardwoodParquetDatasetGenerator.rowAt(0), secondRows.get(0));

        firstRows.clear();
        assertEquals(ROW_COUNT, secondRows.size());
        assertEquals(ROW_COUNT, firstStore.size());
        assertEquals(ROW_COUNT, secondStore.size());
    }

    private static void assertSealed(ProjectionStore<HardwoodMarketDataProjection> store) {
        store.cursor();
        assertThrows(
                IllegalStateException.class,
                () -> store.add(HardwoodParquetDatasetGenerator.rowAt(0)));
    }

    private static void assertSelectedRows(
            ProjectionStore<HardwoodMarketDataProjection> store,
            ArrayList<HardwoodMarketDataRow> rows,
            int... rowIndexes) {
        for (int rowIndex : rowIndexes) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            assertProjectionEquals(expected, store.viewAt(rowIndex), "selected columnar row " + rowIndex);
            assertEquals(expected, rows.get(rowIndex), "selected ArrayList row " + rowIndex);
        }
    }

    private static void assertStableStringReferences(
            ProjectionStore<HardwoodMarketDataProjection> store,
            ArrayList<HardwoodMarketDataRow> rows,
            int rowIndex) {
        HardwoodMarketDataProjection firstView = store.viewAt(rowIndex);
        HardwoodMarketDataProjection secondView = store.viewAt(rowIndex);
        assertSame(firstView.symbol(), secondView.symbol());
        assertSame(firstView.venue(), secondView.venue());
        assertSame(firstView.side(), secondView.side());

        HardwoodMarketDataRow row = rows.get(rowIndex);
        assertSame(row.symbol(), row.symbol());
        assertSame(row.venue(), row.venue());
        assertSame(row.side(), row.side());
    }

    private static void assertProjectionEquals(
            HardwoodMarketDataProjection expected,
            HardwoodMarketDataProjection actual,
            String context) {
        assertEquals(expected.timestamp(), actual.timestamp(), context + " timestamp");
        assertEquals(expected.symbol(), actual.symbol(), context + " symbol");
        assertEquals(expected.venue(), actual.venue(), context + " venue");
        assertEquals(expected.side(), actual.side(), context + " side");
        assertEquals(expected.sequenceNumber(), actual.sequenceNumber(), context + " sequenceNumber");
        assertEquals(expected.bidPrice(), actual.bidPrice(), context + " bidPrice");
        assertEquals(expected.askPrice(), actual.askPrice(), context + " askPrice");
        assertEquals(expected.lastTradePrice(), actual.lastTradePrice(), context + " lastTradePrice");
    }
}
