package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class HardwoodDestinationMaterializationCasesTest {

    private static final int PROJECTION_COLUMN_COUNT = 8;
    private static final int ROW_COUNT = 19;
    private static final int BATCH_SIZE = 7;

    @Test
    void allPathsPreserveOrderingAndMaterializeTheSameSourceArrays() throws Exception {
        HardwoodDestinationMaterializationCases.SourceArrays source =
                HardwoodDestinationMaterializationCases.createSourceArrays(ROW_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(PROJECTION_COLUMN_COUNT);
        try {
            HardwoodMarketDataProjectionStore sequentialStore =
                    HardwoodDestinationMaterializationCases.sequentialRangedBatches(
                            source, BATCH_SIZE);
            HardwoodMarketDataProjectionStore perBatchBarrierStore =
                    HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                            source, BATCH_SIZE, executor);
            HardwoodMarketDataProjectionStore pipelinedStore =
                    HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                            source, BATCH_SIZE, executor);
            ArrayList<HardwoodMarketDataRow> rows =
                    HardwoodDestinationMaterializationCases.arrayListRows(
                            source, BATCH_SIZE);

            assertEquals(ROW_COUNT, sequentialStore.size());
            assertEquals(ROW_COUNT, perBatchBarrierStore.size());
            assertEquals(ROW_COUNT, pipelinedStore.size());
            assertEquals(ROW_COUNT, rows.size());
            assertEquals(ROW_COUNT, storeCapacity(sequentialStore));
            assertEquals(ROW_COUNT, storeCapacity(perBatchBarrierStore));
            assertEquals(ROW_COUNT, storeCapacity(pipelinedStore));

            assertRowsInSourceOrder(sequentialStore, perBatchBarrierStore, pipelinedStore, rows);
            assertSealed(sequentialStore);
            assertSealed(perBatchBarrierStore);
            assertSealed(pipelinedStore);
            assertEquals("caller still owns executor",
                    executor.submit(() -> "caller still owns executor").get());
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private static void assertRowsInSourceOrder(
            HardwoodMarketDataProjectionStore sequentialStore,
            HardwoodMarketDataProjectionStore perBatchBarrierStore,
            HardwoodMarketDataProjectionStore pipelinedStore,
            ArrayList<HardwoodMarketDataRow> rows) {
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            assertProjectionEquals(expected, sequentialStore.viewAt(rowIndex));
            assertProjectionEquals(expected, perBatchBarrierStore.viewAt(rowIndex));
            assertProjectionEquals(expected, pipelinedStore.viewAt(rowIndex));
            assertEquals(expected, rows.get(rowIndex));
        }
    }

    private static int storeCapacity(HardwoodMarketDataProjectionStore store)
            throws Exception {
        Field capacity = store.getClass().getDeclaredField("capacity");
        capacity.setAccessible(true);
        return capacity.getInt(store);
    }

    private static void assertSealed(HardwoodMarketDataProjectionStore store) {
        assertThrows(
                IllegalStateException.class,
                () -> store.add(HardwoodParquetDatasetGenerator.rowAt(0)));
    }

    private static void assertProjectionEquals(
            HardwoodMarketDataProjection expected,
            HardwoodMarketDataProjection actual) {
        assertEquals(expected.timestamp(), actual.timestamp());
        assertEquals(expected.symbol(), actual.symbol());
        assertEquals(expected.venue(), actual.venue());
        assertEquals(expected.side(), actual.side());
        assertEquals(expected.sequenceNumber(), actual.sequenceNumber());
        assertEquals(expected.bidPrice(), actual.bidPrice());
        assertEquals(expected.askPrice(), actual.askPrice());
        assertEquals(expected.lastTradePrice(), actual.lastTradePrice());
    }
}
