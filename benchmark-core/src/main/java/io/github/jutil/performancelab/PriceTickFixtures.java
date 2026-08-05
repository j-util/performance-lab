package io.github.jutil.performancelab;

/** Deterministic, filesystem-independent input values for price-average benchmarks. */
final class PriceTickFixtures {

    private static final long BASE_TIMESTAMP_MILLIS = 1_704_067_200_000L;
    private static final long PRICE_MULTIPLIER = 7_919L;
    private static final long PRICE_RANGE_CENTS = 49_802L;
    private static final double MINIMUM_PRICE = 1.99d;

    private PriceTickFixtures() {
    }

    static PriceTick tickAt(int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("Row index must not be negative: " + rowIndex);
        }
        long timestamp = BASE_TIMESTAMP_MILLIS + rowIndex * 1_000L;
        double price = MINIMUM_PRICE
                + ((rowIndex * PRICE_MULTIPLIER) % PRICE_RANGE_CENTS) / 100.0d;
        return new PriceTick(timestamp, price);
    }

    static double expectedAverage(int rowCount) {
        validateRowCount(rowCount);
        double sum = 0.0d;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            sum += tickAt(rowIndex).price();
        }
        return sum / rowCount;
    }

    static void validateRowCount(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Row count must be positive: " + rowCount);
        }
    }
}
