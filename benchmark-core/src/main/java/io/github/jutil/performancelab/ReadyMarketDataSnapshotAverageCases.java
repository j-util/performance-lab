package io.github.jutil.performancelab;

import java.util.ArrayList;

import org.eclipse.collections.impl.list.mutable.FastList;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

/** Construction, calculation, and validation for ready market-data snapshot cases. */
final class ReadyMarketDataSnapshotAverageCases {

    static final String CAPTURED_AT_NANOS_COLUMN = "capturedAtNanos";
    static final String SYMBOL_COLUMN = "symbol";
    static final String LAST_TRADE_PRICE_COLUMN = "lastTradePrice";
    static final String LAST_TRADE_SIZE_COLUMN = "lastTradeSize";
    static final String BID_PRICE_COLUMN = "bidPrice";
    static final String ASK_PRICE_COLUMN = "askPrice";
    static final String BID_SIZE_COLUMN = "bidSize";
    static final String ASK_SIZE_COLUMN = "askSize";

    private static final double RELATIVE_TOLERANCE = 1.0e-12d;
    private static final int ULP_TOLERANCE = 16;

    private ReadyMarketDataSnapshotAverageCases() {
    }

    static ArrayList<MarketDataSnapshot> newArrayList(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        ArrayList<MarketDataSnapshot> rows = new ArrayList<>(rowCount);
        addSnapshots(rowCount, rows::add);
        validateSize("ArrayList", rowCount, rows.size());
        return rows;
    }

    static ObjectArrayList<MarketDataSnapshot> newFastUtilObjectArrayList(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        ObjectArrayList<MarketDataSnapshot> rows = new ObjectArrayList<>(rowCount);
        addSnapshots(rowCount, rows::add);
        validateSize("FastUtil ObjectArrayList", rowCount, rows.size());
        return rows;
    }

    static FastList<MarketDataSnapshot> newEclipseFastList(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        FastList<MarketDataSnapshot> rows = new FastList<>(rowCount);
        addSnapshots(rowCount, rows::add);
        validateSize("Eclipse Collections FastList", rowCount, rows.size());
        return rows;
    }

    static Table newTablesawTable(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        long[] capturedAtNanos = new long[rowCount];
        String[] symbols = new String[rowCount];
        double[] lastTradePrices = new double[rowCount];
        double[] lastTradeSizes = new double[rowCount];
        double[] bidPrices = new double[rowCount];
        double[] askPrices = new double[rowCount];
        double[] bidSizes = new double[rowCount];
        double[] askSizes = new double[rowCount];

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            capturedAtNanos[rowIndex] = snapshot.capturedAtNanos();
            symbols[rowIndex] = snapshot.symbol();
            lastTradePrices[rowIndex] = snapshot.lastTradePrice();
            lastTradeSizes[rowIndex] = snapshot.lastTradeSize();
            bidPrices[rowIndex] = snapshot.bidPrice();
            askPrices[rowIndex] = snapshot.askPrice();
            bidSizes[rowIndex] = snapshot.bidSize();
            askSizes[rowIndex] = snapshot.askSize();
        }

        Table table = Table.create(
                "market data snapshots",
                LongColumn.create(CAPTURED_AT_NANOS_COLUMN, capturedAtNanos),
                StringColumn.create(SYMBOL_COLUMN, symbols),
                DoubleColumn.create(LAST_TRADE_PRICE_COLUMN, lastTradePrices),
                DoubleColumn.create(LAST_TRADE_SIZE_COLUMN, lastTradeSizes),
                DoubleColumn.create(BID_PRICE_COLUMN, bidPrices),
                DoubleColumn.create(ASK_PRICE_COLUMN, askPrices),
                DoubleColumn.create(BID_SIZE_COLUMN, bidSizes),
                DoubleColumn.create(ASK_SIZE_COLUMN, askSizes));
        validateSize("Tablesaw Table", rowCount, table.rowCount());
        if (table.columnCount() != 8) {
            throw new IllegalStateException(
                    "Tablesaw Table contains " + table.columnCount() + " columns; expected 8");
        }
        return table;
    }

    /**
     * Creates the calculation-only last-trade-price baseline. Like the narrow price-average
     * suite's baseline, this is deliberately not a complete-record representation.
     */
    static double[] newDoubleArrayBaselineLastTradePrices(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        double[] lastTradePrices = new double[rowCount];
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            lastTradePrices[rowIndex] =
                    MarketDataSnapshotFixtures.snapshotAt(rowIndex).lastTradePrice();
        }
        validateSize("Double-array baseline last-trade prices", rowCount, lastTradePrices.length);
        return lastTradePrices;
    }

    static ProjectionStore<MarketDataSnapshotProjection> newColumnarProjectionStore(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        ProjectionStore<MarketDataSnapshotProjection> store =
                ProjectionStores.create(MarketDataSnapshotProjection.class, rowCount);
        addSnapshots(rowCount, store::add);
        validateSize("Columnar Projection Store", rowCount, store.size());
        store.seal();
        return store;
    }

    static double arrayListLastTradePriceAverage(ArrayList<MarketDataSnapshot> rows) {
        double sum = 0.0d;
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += rows.get(index).lastTradePrice();
        }
        return sum / rows.size();
    }

    static double fastUtilObjectArrayListLastTradePriceAverage(
            ObjectArrayList<MarketDataSnapshot> rows) {
        double sum = 0.0d;
        Object[] elements = rows.elements();
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += ((MarketDataSnapshot) elements[index]).lastTradePrice();
        }
        return sum / rows.size();
    }

    static double eclipseFastListLastTradePriceAverage(FastList<MarketDataSnapshot> rows) {
        return rows.sumOfDouble(MarketDataSnapshot::lastTradePrice) / rows.size();
    }

    static double eclipseFastListNaiveLastTradePriceAverage(FastList<MarketDataSnapshot> rows) {
        double sum = 0.0d;
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += rows.get(index).lastTradePrice();
        }
        return sum / rows.size();
    }

    static double tablesawTableLastTradePriceAverage(Table table) {
        return table.doubleColumn(LAST_TRADE_PRICE_COLUMN).mean();
    }

    static double tablesawTableNaiveLastTradePriceAverage(Table table) {
        DoubleColumn lastTradePrices = table.doubleColumn(LAST_TRADE_PRICE_COLUMN);
        double sum = 0.0d;
        for (int index = 0, size = lastTradePrices.size(); index < size; index++) {
            sum += lastTradePrices.getDouble(index);
        }
        return sum / lastTradePrices.size();
    }

    static double columnarProjectionStoreLastTradePriceAverage(
            ProjectionStore<MarketDataSnapshotProjection> store) {
        double sum = 0.0d;
        ProjectionCursor<MarketDataSnapshotProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            sum += cursor.current().lastTradePrice();
        }
        return sum / store.size();
    }

    static double doubleArrayBaselineLastTradePriceAverage(double[] lastTradePrices) {
        double sum = 0.0d;
        for (double lastTradePrice : lastTradePrices) {
            sum += lastTradePrice;
        }
        return sum / lastTradePrices.length;
    }

    static void validateAverage(String representation, int rowCount, double actualAverage) {
        double expectedAverage =
                MarketDataSnapshotFixtures.expectedLastTradePriceAverage(rowCount);
        double tolerance = toleranceFor(expectedAverage);
        if (!Double.isFinite(actualAverage)
                || Math.abs(expectedAverage - actualAverage) > tolerance) {
            throw new IllegalStateException(
                    representation + " average was " + actualAverage + "; expected "
                            + expectedAverage + " within " + tolerance);
        }
    }

    static double toleranceFor(double expectedAverage) {
        return Math.max(
                Math.abs(expectedAverage) * RELATIVE_TOLERANCE,
                Math.ulp(expectedAverage) * ULP_TOLERANCE);
    }

    private static void addSnapshots(
            int rowCount, java.util.function.Consumer<MarketDataSnapshot> destination) {
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            destination.accept(MarketDataSnapshotFixtures.snapshotAt(rowIndex));
        }
    }

    private static void validateSize(String representation, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    representation + " contains " + actual + " rows; expected " + expected);
        }
    }
}
