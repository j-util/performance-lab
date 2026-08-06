package io.github.jutil.performancelab;

import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.ObjectArrayList;

/** Complete market-data snapshots stored as manually assembled HPPC columns. */
final class HppcMarketDataSnapshotColumns {

    private final LongArrayList capturedAtNanos;
    private final ObjectArrayList<String> symbols;
    private final DoubleArrayList lastTradePrices;
    private final DoubleArrayList lastTradeSizes;
    private final DoubleArrayList bidPrices;
    private final DoubleArrayList askPrices;
    private final DoubleArrayList bidSizes;
    private final DoubleArrayList askSizes;

    private HppcMarketDataSnapshotColumns(
            LongArrayList capturedAtNanos,
            ObjectArrayList<String> symbols,
            DoubleArrayList lastTradePrices,
            DoubleArrayList lastTradeSizes,
            DoubleArrayList bidPrices,
            DoubleArrayList askPrices,
            DoubleArrayList bidSizes,
            DoubleArrayList askSizes) {
        this.capturedAtNanos = capturedAtNanos;
        this.symbols = symbols;
        this.lastTradePrices = lastTradePrices;
        this.lastTradeSizes = lastTradeSizes;
        this.bidPrices = bidPrices;
        this.askPrices = askPrices;
        this.bidSizes = bidSizes;
        this.askSizes = askSizes;
    }

    static HppcMarketDataSnapshotColumns fromFixture(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        LongArrayList capturedAtNanos = new LongArrayList(rowCount);
        ObjectArrayList<String> symbols = new ObjectArrayList<>(rowCount);
        DoubleArrayList lastTradePrices = new DoubleArrayList(rowCount);
        DoubleArrayList lastTradeSizes = new DoubleArrayList(rowCount);
        DoubleArrayList bidPrices = new DoubleArrayList(rowCount);
        DoubleArrayList askPrices = new DoubleArrayList(rowCount);
        DoubleArrayList bidSizes = new DoubleArrayList(rowCount);
        DoubleArrayList askSizes = new DoubleArrayList(rowCount);

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            MarketDataSnapshot snapshot = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            capturedAtNanos.add(snapshot.capturedAtNanos());
            symbols.add(snapshot.symbol());
            lastTradePrices.add(snapshot.lastTradePrice());
            lastTradeSizes.add(snapshot.lastTradeSize());
            bidPrices.add(snapshot.bidPrice());
            askPrices.add(snapshot.askPrice());
            bidSizes.add(snapshot.bidSize());
            askSizes.add(snapshot.askSize());
        }

        validateColumnSize("capturedAtNanos", rowCount, capturedAtNanos.size());
        validateColumnSize("symbol", rowCount, symbols.size());
        validateColumnSize("lastTradePrice", rowCount, lastTradePrices.size());
        validateColumnSize("lastTradeSize", rowCount, lastTradeSizes.size());
        validateColumnSize("bidPrice", rowCount, bidPrices.size());
        validateColumnSize("askPrice", rowCount, askPrices.size());
        validateColumnSize("bidSize", rowCount, bidSizes.size());
        validateColumnSize("askSize", rowCount, askSizes.size());

        return new HppcMarketDataSnapshotColumns(
                capturedAtNanos,
                symbols,
                lastTradePrices,
                lastTradeSizes,
                bidPrices,
                askPrices,
                bidSizes,
                askSizes);
    }

    int rowCount() {
        return lastTradePrices.size();
    }

    int columnCount() {
        return 8;
    }

    MarketDataSnapshot snapshotAt(int rowIndex) {
        return new MarketDataSnapshot(
                capturedAtNanos.get(rowIndex),
                symbols.get(rowIndex),
                lastTradePrices.get(rowIndex),
                lastTradeSizes.get(rowIndex),
                bidPrices.get(rowIndex),
                askPrices.get(rowIndex),
                bidSizes.get(rowIndex),
                askSizes.get(rowIndex));
    }

    double lastTradePriceAverage() {
        double sum = 0.0d;
        for (int rowIndex = 0, size = lastTradePrices.size(); rowIndex < size; rowIndex++) {
            sum += lastTradePrices.get(rowIndex);
        }
        return sum / rowCount();
    }

    private static void validateColumnSize(String column, int expected, int actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    "HPPC " + column + " column contains " + actual + " rows; expected "
                            + expected);
        }
    }
}
