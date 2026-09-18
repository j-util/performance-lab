package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.jutil.columnarprojection.ProjectionStore;

class ColumnarProjectionStoreIterationCasesTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 1000})
    void cursorAndIndexedTraversalProduceIdenticalResultsAndResetAccumulators(int rowCount) {
        ProjectionStore<MarketDataSnapshotProjection> store =
                ColumnarProjectionStoreIterationCases.newStore(rowCount);
        ColumnarProjectionStoreIterationCases.LastTradePriceSumAccumulator sumAccumulator =
                new ColumnarProjectionStoreIterationCases.LastTradePriceSumAccumulator();
        ColumnarProjectionStoreIterationCases.FullRowChecksumAccumulator checksumAccumulator =
                new ColumnarProjectionStoreIterationCases.FullRowChecksumAccumulator();

        double cursorSum = ColumnarProjectionStoreIterationCases
                .cursorLastTradePriceSum(store, sumAccumulator);
        assertEquals(rowCount, sumAccumulator.count());
        double indexedSum = ColumnarProjectionStoreIterationCases
                .indexedStableViewLastTradePriceSum(store, sumAccumulator);
        assertEquals(rowCount, sumAccumulator.count());

        assertEquals(cursorSum, indexedSum);

        long cursorChecksum = ColumnarProjectionStoreIterationCases
                .cursorFullRowChecksum(store, checksumAccumulator);
        assertEquals(rowCount, checksumAccumulator.count());
        long indexedChecksum = ColumnarProjectionStoreIterationCases
                .indexedStableViewFullRowChecksum(store, checksumAccumulator);
        assertEquals(rowCount, checksumAccumulator.count());
        assertEquals(cursorChecksum, indexedChecksum);

        assertEquals(
                cursorSum,
                ColumnarProjectionStoreIterationCases
                        .cursorLastTradePriceSum(store, sumAccumulator));
        assertEquals(rowCount, sumAccumulator.count());
        assertEquals(
                indexedSum,
                ColumnarProjectionStoreIterationCases
                        .indexedStableViewLastTradePriceSum(store, sumAccumulator));
        assertEquals(rowCount, sumAccumulator.count());
        assertEquals(
                cursorChecksum,
                ColumnarProjectionStoreIterationCases
                        .cursorFullRowChecksum(store, checksumAccumulator));
        assertEquals(rowCount, checksumAccumulator.count());
        assertEquals(
                indexedChecksum,
                ColumnarProjectionStoreIterationCases
                        .indexedStableViewFullRowChecksum(store, checksumAccumulator));
        assertEquals(rowCount, checksumAccumulator.count());
        ColumnarProjectionStoreIterationCases.validate(store, rowCount);
    }
}
