package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

class OffHeapMarketDataSnapshotStoresTest {

    @Test
    void oneLogicalRecordPreservesEveryFieldAndMatchesOrdinaryAverage() {
        assertCompleteEquivalentStores(1);
    }

    @Test
    void multipleLogicalRecordsPreserveEveryFieldOrderAndOrdinaryAverage() {
        assertCompleteEquivalentStores(37);
    }

    @Test
    void rejectsNonPositiveLogicalRowCounts() {
        for (int invalidRowCount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MemorySegmentMarketDataSnapshotStore.fromFixture(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ChronicleValuesBytesMarketDataSnapshotStore
                            .fromFixture(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ApacheArrowMarketDataSnapshotStore.fromFixture(invalidRowCount));
        }
    }

    @Test
    void memorySegmentRejectsOverCapacitySymbolAndCleansUpPartialConstruction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MemorySegmentMarketDataSnapshotStore.fromSnapshots(
                        2, fixtureThenSnapshotWithSymbol("ABCDEFGH")));
    }

    @Test
    void chronicleValuesRejectsOverCapacitySymbolAndCleansUpPartialConstruction() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ChronicleValuesBytesMarketDataSnapshotStore.fromSnapshots(
                        2, fixtureThenSnapshotWithSymbol("ABCDEFGH")));
    }

    @Test
    void apacheArrowCleansUpPartialConstructionFailure() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ApacheArrowMarketDataSnapshotStore.fromSnapshots(
                        2, fixtureThenSnapshotWithSymbol("ABCDEFGH")));
    }

    @Test
    void resourcesCloseNormallyAndRepeatedTeardownIsSafe() {
        MemorySegmentMarketDataSnapshotStore memorySegment =
                MemorySegmentMarketDataSnapshotStore.fromFixture(1);
        ChronicleValuesBytesMarketDataSnapshotStore chronicle =
                ChronicleValuesBytesMarketDataSnapshotStore.fromFixture(1);
        ApacheArrowMarketDataSnapshotStore arrow =
                ApacheArrowMarketDataSnapshotStore.fromFixture(1);

        memorySegment.close();
        chronicle.close();
        arrow.close();
        memorySegment.close();
        chronicle.close();
        arrow.close();
    }

    private static void assertCompleteEquivalentStores(int rowCount) {
        ArrayList<MarketDataSnapshot> ordinaryRows =
                ReadyMarketDataSnapshotAverageCases.newArrayList(rowCount);
        double expectedAverage = ReadyMarketDataSnapshotAverageCases
                .arrayListLastTradePriceAverage(ordinaryRows);

        try (MemorySegmentMarketDataSnapshotStore memorySegment =
                        MemorySegmentMarketDataSnapshotStore.fromFixture(rowCount);
                ChronicleValuesBytesMarketDataSnapshotStore chronicle =
                        ChronicleValuesBytesMarketDataSnapshotStore.fromFixture(rowCount);
                ApacheArrowMarketDataSnapshotStore arrow =
                        ApacheArrowMarketDataSnapshotStore.fromFixture(rowCount)) {
            assertEquals(rowCount, memorySegment.rowCount());
            assertEquals(rowCount, chronicle.rowCount());
            assertEquals(rowCount, arrow.rowCount());
            assertEquals(64L, MemorySegmentMarketDataSnapshotStore.ROW_STRIDE);
            assertEquals(8L, MemorySegmentMarketDataSnapshotStore.ROW_ALIGNMENT);
            assertTrue(chronicle.rowStride() > 0L);
            assertTrue(chronicle.isDirectMemory());
            assertEquals(8, arrow.columnCount());

            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                MarketDataSnapshot expected = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
                assertEquals(expected, memorySegment.snapshotAt(rowIndex));
                assertEquals(expected, chronicle.snapshotAt(rowIndex));
                assertEquals(expected, arrow.snapshotAt(rowIndex));
            }

            assertEquals(expectedAverage, memorySegment.lastTradePriceAverage());
            assertEquals(expectedAverage, chronicle.lastTradePriceAverage());
            assertEquals(expectedAverage, arrow.lastTradePriceAverage());
        }
    }

    private static IntFunction<MarketDataSnapshot> fixtureThenSnapshotWithSymbol(String symbol) {
        return rowIndex -> {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            if (rowIndex == 0) {
                return snapshot;
            }
            return new MarketDataSnapshot(
                    snapshot.capturedAtNanos(),
                    symbol,
                    snapshot.lastTradePrice(),
                    snapshot.lastTradeSize(),
                    snapshot.bidPrice(),
                    snapshot.askPrice(),
                    snapshot.bidSize(),
                    snapshot.askSize());
        };
    }
}
