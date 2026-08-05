package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.jupiter.api.Test;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tech.tablesaw.api.Table;

class ReadyMarketDataSnapshotAverageCasesTest {

    @Test
    void generationIsDeterministicValidAndRepresentsDistinctCompletedTrades() {
        MarketDataSnapshot first = MarketDataSnapshotFixtures.snapshotAt(0);
        MarketDataSnapshot sameFirst = MarketDataSnapshotFixtures.snapshotAt(0);
        MarketDataSnapshot second = MarketDataSnapshotFixtures.snapshotAt(1);

        assertEquals(first, sameFirst);
        assertEquals("AAPL", first.symbol());
        assertEquals(179.50d, first.lastTradePrice());
        assertNotEquals(first.capturedAtNanos(), second.capturedAtNanos());
        assertNotEquals(first.lastTradePrice(), second.lastTradePrice());
        assertThrows(IllegalArgumentException.class, () -> MarketDataSnapshotFixtures.snapshotAt(-1));

        long previousCapturedAtNanos = -1L;
        for (int rowIndex = 0; rowIndex < 10_000; rowIndex++) {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            assertTrue(snapshot.capturedAtNanos() > previousCapturedAtNanos);
            assertFinitePositive(snapshot.lastTradePrice());
            assertFinitePositive(snapshot.lastTradeSize());
            assertFinitePositive(snapshot.bidPrice());
            assertFinitePositive(snapshot.askPrice());
            assertFinitePositive(snapshot.bidSize());
            assertFinitePositive(snapshot.askSize());
            assertTrue(snapshot.bidPrice() <= snapshot.lastTradePrice());
            assertTrue(snapshot.lastTradePrice() <= snapshot.askPrice());
            previousCapturedAtNanos = snapshot.capturedAtNanos();
        }
    }

    @Test
    void fixtureReusesOnlyItsFixedSymbolPool() {
        Set<String> symbolsByIdentity =
                Collections.newSetFromMap(new IdentityHashMap<>());
        int poolSize = MarketDataSnapshotFixtures.symbolPoolSize();
        for (int rowIndex = 0; rowIndex < 10_000; rowIndex++) {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            symbolsByIdentity.add(snapshot.symbol());
            if (rowIndex >= poolSize) {
                assertSame(
                        MarketDataSnapshotFixtures.snapshotAt(rowIndex - poolSize).symbol(),
                        snapshot.symbol());
            }
        }

        assertEquals(poolSize, symbolsByIdentity.size());
        assertTrue(poolSize < 10_000);
    }

    @Test
    void allComparatorsProduceEquivalentAverageForSingleRecord() {
        assertEquivalentComparators(1);
    }

    @Test
    void completeRepresentationsAndDoubleArrayBaselinePreserveFixtureData() {
        assertEquivalentComparators(37);
    }

    @Test
    void rejectsNonPositiveRowCounts() {
        for (int invalidRowCount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases.newArrayList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases
                            .newFastUtilObjectArrayList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases.newEclipseFastList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases.newTablesawTable(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases
                            .newDoubleArrayBaselineLastTradePrices(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases
                            .newColumnarProjectionStore(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MarketDataSnapshotFixtures
                            .expectedLastTradePriceAverage(invalidRowCount));
        }
    }

    private static void assertEquivalentComparators(int rowCount) {
        List<MarketDataSnapshot> expectedRows = new ArrayList<>(rowCount);
        double[] expectedLastTradePrices = new double[rowCount];
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            expectedRows.add(snapshot);
            expectedLastTradePrices[rowIndex] = snapshot.lastTradePrice();
        }

        ArrayList<MarketDataSnapshot> arrayList =
                ReadyMarketDataSnapshotAverageCases.newArrayList(rowCount);
        ObjectArrayList<MarketDataSnapshot> fastUtil =
                ReadyMarketDataSnapshotAverageCases.newFastUtilObjectArrayList(rowCount);
        FastList<MarketDataSnapshot> eclipse =
                ReadyMarketDataSnapshotAverageCases.newEclipseFastList(rowCount);
        Table tablesaw = ReadyMarketDataSnapshotAverageCases.newTablesawTable(rowCount);
        ProjectionStore<MarketDataSnapshotProjection> columnar =
                ReadyMarketDataSnapshotAverageCases.newColumnarProjectionStore(rowCount);
        double[] baselineLastTradePrices = ReadyMarketDataSnapshotAverageCases
                .newDoubleArrayBaselineLastTradePrices(rowCount);

        assertEquals(expectedRows, arrayList);
        assertEquals(expectedRows, new ArrayList<>(fastUtil));
        assertEquals(expectedRows, new ArrayList<>(eclipse));
        assertEquals(expectedRows, snapshot(tablesaw));
        assertEquals(expectedRows, snapshot(columnar));
        assertArrayEquals(expectedLastTradePrices, baselineLastTradePrices);

        double expectedAverage =
                MarketDataSnapshotFixtures.expectedLastTradePriceAverage(rowCount);
        double tolerance = ReadyMarketDataSnapshotAverageCases.toleranceFor(expectedAverage);
        double arrayListAverage =
                ReadyMarketDataSnapshotAverageCases.arrayListLastTradePriceAverage(arrayList);
        double fastUtilAverage = ReadyMarketDataSnapshotAverageCases
                .fastUtilObjectArrayListLastTradePriceAverage(fastUtil);
        double eclipseNaiveAverage = ReadyMarketDataSnapshotAverageCases
                .eclipseFastListNaiveLastTradePriceAverage(eclipse);
        double tablesawNaiveAverage = ReadyMarketDataSnapshotAverageCases
                .tablesawTableNaiveLastTradePriceAverage(tablesaw);
        double columnarAverage = ReadyMarketDataSnapshotAverageCases
                .columnarProjectionStoreLastTradePriceAverage(columnar);
        double baselineAverage = ReadyMarketDataSnapshotAverageCases
                .doubleArrayBaselineLastTradePriceAverage(baselineLastTradePrices);

        assertEquals(expectedAverage, arrayListAverage, tolerance);
        assertEquals(arrayListAverage, fastUtilAverage);
        assertEquals(arrayListAverage, eclipseNaiveAverage);
        assertEquals(arrayListAverage, tablesawNaiveAverage);
        assertEquals(arrayListAverage, columnarAverage);
        assertEquals(arrayListAverage, baselineAverage);

        assertEquals(
                expectedAverage,
                ReadyMarketDataSnapshotAverageCases
                        .eclipseFastListLastTradePriceAverage(eclipse),
                tolerance);
        assertEquals(
                expectedAverage,
                ReadyMarketDataSnapshotAverageCases.tablesawTableLastTradePriceAverage(tablesaw),
                tolerance);
    }

    private static List<MarketDataSnapshot> snapshot(Table table) {
        assertEquals(8, table.columnCount());
        List<MarketDataSnapshot> snapshots = new ArrayList<>(table.rowCount());
        for (int rowIndex = 0; rowIndex < table.rowCount(); rowIndex++) {
            snapshots.add(new MarketDataSnapshot(
                    table.longColumn(ReadyMarketDataSnapshotAverageCases.CAPTURED_AT_NANOS_COLUMN)
                            .getLong(rowIndex),
                    table.stringColumn(ReadyMarketDataSnapshotAverageCases.SYMBOL_COLUMN)
                            .get(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_PRICE_COLUMN)
                            .getDouble(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_SIZE_COLUMN)
                            .getDouble(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.BID_PRICE_COLUMN)
                            .getDouble(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.ASK_PRICE_COLUMN)
                            .getDouble(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.BID_SIZE_COLUMN)
                            .getDouble(rowIndex),
                    table.doubleColumn(ReadyMarketDataSnapshotAverageCases.ASK_SIZE_COLUMN)
                            .getDouble(rowIndex)));
        }
        return snapshots;
    }

    private static List<MarketDataSnapshot> snapshot(
            ProjectionStore<MarketDataSnapshotProjection> store) {
        List<MarketDataSnapshot> snapshots = new ArrayList<>(store.size());
        ProjectionCursor<MarketDataSnapshotProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            MarketDataSnapshotProjection snapshot = cursor.current();
            snapshots.add(new MarketDataSnapshot(
                    snapshot.capturedAtNanos(),
                    snapshot.symbol(),
                    snapshot.lastTradePrice(),
                    snapshot.lastTradeSize(),
                    snapshot.bidPrice(),
                    snapshot.askPrice(),
                    snapshot.bidSize(),
                    snapshot.askSize()));
        }
        return snapshots;
    }

    private static void assertFinitePositive(double value) {
        assertTrue(Double.isFinite(value));
        assertTrue(value > 0.0d);
    }
}
