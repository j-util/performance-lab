package io.github.jutil.performancelab;

/** Deterministic, filesystem-independent input for market-data snapshot benchmarks. */
final class MarketDataSnapshotFixtures {

    private static final long BASE_CAPTURED_AT_NANOS = 1_704_067_200_000_000_000L;
    private static final long CAPTURE_INTERVAL_NANOS = 50_000L;
    private static final long PRICE_MULTIPLIER = 7_919L;
    private static final long PRICE_OFFSET_RANGE_CENTS = 2_001L;
    private static final String[] SYMBOLS = {
        "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "JPM"
    };
    private static final double[] BASE_PRICES = {
        189.50d, 415.25d, 875.75d, 178.40d, 142.80d, 505.60d, 240.15d, 192.30d
    };

    private MarketDataSnapshotFixtures() {
    }

    static MarketDataSnapshot snapshotAt(int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("Row index must not be negative: " + rowIndex);
        }

        int symbolIndex = rowIndex % SYMBOLS.length;
        double lastTradePrice = BASE_PRICES[symbolIndex]
                + (((long) rowIndex * PRICE_MULTIPLIER) % PRICE_OFFSET_RANGE_CENTS - 1_000L)
                        / 100.0d;
        double bidPrice = lastTradePrice
                - (0.01d + ((long) rowIndex * 29L % 25L) / 100.0d);
        double askPrice = lastTradePrice
                + (0.01d + (((long) rowIndex * 31L + 7L) % 25L) / 100.0d);
        double lastTradeSize = 1.0d + ((long) rowIndex * 37L % 50_000L) / 10.0d;
        double bidSize = 10.0d + (((long) rowIndex * 53L + 17L) % 100_000L) / 10.0d;
        double askSize = 10.0d + (((long) rowIndex * 71L + 31L) % 100_000L) / 10.0d;

        return new MarketDataSnapshot(
                BASE_CAPTURED_AT_NANOS + (long) rowIndex * CAPTURE_INTERVAL_NANOS,
                SYMBOLS[symbolIndex],
                lastTradePrice,
                lastTradeSize,
                bidPrice,
                askPrice,
                bidSize,
                askSize);
    }

    static double expectedLastTradePriceAverage(int rowCount) {
        validateRowCount(rowCount);
        double sum = 0.0d;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            sum += snapshotAt(rowIndex).lastTradePrice();
        }
        return sum / rowCount;
    }

    static int symbolPoolSize() {
        return SYMBOLS.length;
    }

    static void validateRowCount(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Row count must be positive: " + rowCount);
        }
    }
}
