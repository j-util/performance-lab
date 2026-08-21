package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
        ExecutorService fixedExecutor =
                Executors.newFixedThreadPool(PROJECTION_COLUMN_COUNT);
        ExecutorService virtualThreadPerTaskExecutor =
                Executors.newVirtualThreadPerTaskExecutor();
        try {
            HardwoodMarketDataProjectionStore sequentialStore =
                    HardwoodDestinationMaterializationCases.sequentialRangedBatches(
                            source, BATCH_SIZE);
            HardwoodMarketDataProjectionStore fixedPerBatchBarrierStore =
                    HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                            source, BATCH_SIZE, fixedExecutor);
            HardwoodMarketDataProjectionStore fixedPipelinedStore =
                    HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                            source, BATCH_SIZE, fixedExecutor);
            HardwoodMarketDataProjectionStore virtualThreadPerTaskPerBatchBarrierStore =
                    HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                            source, BATCH_SIZE, virtualThreadPerTaskExecutor);
            HardwoodMarketDataProjectionStore virtualThreadPerTaskPipelinedStore =
                    HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                            source, BATCH_SIZE, virtualThreadPerTaskExecutor);
            ArrayList<HardwoodMarketDataRow> rows =
                    HardwoodDestinationMaterializationCases.arrayListRows(
                            source, BATCH_SIZE);

            List<HardwoodMarketDataProjectionStore> stores = List.of(
                    sequentialStore,
                    fixedPerBatchBarrierStore,
                    fixedPipelinedStore,
                    virtualThreadPerTaskPerBatchBarrierStore,
                    virtualThreadPerTaskPipelinedStore);
            for (HardwoodMarketDataProjectionStore store : stores) {
                assertEquals(ROW_COUNT, store.size());
                assertEquals(ROW_COUNT, storeCapacity(store));
                assertSealed(store);
            }
            assertEquals(ROW_COUNT, rows.size());

            assertRowsInSourceOrder(stores, rows);
            assertExecutorStillUsable(fixedExecutor, "fixed-pool executor remains caller-owned");
            assertExecutorStillUsable(
                    virtualThreadPerTaskExecutor,
                    "virtual-thread-per-task executor remains caller-owned");
        } finally {
            fixedExecutor.shutdown();
            virtualThreadPerTaskExecutor.shutdown();
            boolean fixedExecutorTerminated =
                    fixedExecutor.awaitTermination(30, TimeUnit.SECONDS);
            boolean virtualThreadExecutorTerminated =
                    virtualThreadPerTaskExecutor.awaitTermination(30, TimeUnit.SECONDS);
            assertTrue(fixedExecutorTerminated);
            assertTrue(virtualThreadExecutorTerminated);
        }
    }

    private static void assertRowsInSourceOrder(
            List<HardwoodMarketDataProjectionStore> stores,
            ArrayList<HardwoodMarketDataRow> rows) {
        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            for (HardwoodMarketDataProjectionStore store : stores) {
                assertProjectionEquals(expected, store.viewAt(rowIndex));
            }
            assertEquals(expected, rows.get(rowIndex));
        }
    }

    private static void assertExecutorStillUsable(
            ExecutorService executor,
            String message) throws Exception {
        assertEquals(message, executor.submit(() -> message).get());
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
