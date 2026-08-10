package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.columnarprojection.ProjectionStore;

class HardwoodMaterializationCasesTest {

    private static final int ROW_COUNT = 10_001;
    private static final int BATCH_SIZE = 4_096;

    @TempDir
    Path temporaryDirectory;

    @Test
    void allPathsReadEquivalentOrderedRowsAcrossMultipleBatches() throws Exception {
        Path parquetFile = temporaryDirectory.resolve("market-data.parquet");
        HardwoodParquetDatasetGenerator.write(parquetFile, ROW_COUNT);

        long expectedChecksum = HardwoodMaterializationCases.checksumExpectedRows(ROW_COUNT);
        HardwoodMaterializationCases.DirectValidation direct =
                HardwoodMaterializationCases.validateDirect(parquetFile, BATCH_SIZE);
        ProjectionStore<HardwoodMarketDataProjection> batchStore =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ProjectionStore<HardwoodMarketDataProjection> perRowStore =
                HardwoodMaterializationCases.hardwoodToColumnarPerRow(
                        parquetFile, ROW_COUNT, BATCH_SIZE);
        ArrayList<HardwoodMarketDataRow> rows =
                HardwoodMaterializationCases.hardwoodToArrayList(
                        parquetFile, ROW_COUNT, BATCH_SIZE);

        assertEquals(ROW_COUNT, direct.rowCount());
        assertTrue(direct.batchCount() > 1, "the configured read must process multiple batches");
        assertEquals(ROW_COUNT, batchStore.size());
        assertEquals(ROW_COUNT, perRowStore.size());
        assertEquals(ROW_COUNT, rows.size());
        assertEquals(expectedChecksum, direct.checksum());
        assertEquals(expectedChecksum, HardwoodMaterializationCases.checksum(batchStore));
        assertEquals(expectedChecksum, HardwoodMaterializationCases.checksum(perRowStore));
        assertEquals(expectedChecksum, HardwoodMaterializationCases.checksum(rows));

        assertSealed(batchStore);
        assertSealed(perRowStore);
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            HardwoodMarketDataRow batch = copy(batchStore.viewAt(rowIndex));
            HardwoodMarketDataRow perRow = copy(perRowStore.viewAt(rowIndex));
            assertEquals(expected, batch, "batch row " + rowIndex);
            assertEquals(expected, perRow, "per-row row " + rowIndex);
            assertEquals(expected, rows.get(rowIndex), "ArrayList row " + rowIndex);
            assertEquals(batch, perRow, "columnar representations at row " + rowIndex);
        }
    }

    private static void assertSealed(ProjectionStore<HardwoodMarketDataProjection> store) {
        store.cursor();
        assertThrows(
                IllegalStateException.class,
                () -> store.add(HardwoodParquetDatasetGenerator.rowAt(0)));
    }

    private static HardwoodMarketDataRow copy(HardwoodMarketDataProjection row) {
        return new HardwoodMarketDataRow(
                row.timestamp(),
                row.symbol(),
                row.venue(),
                row.side(),
                row.sequenceNumber(),
                row.bidPrice(),
                row.askPrice(),
                row.lastTradePrice());
    }
}
