package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionSchema;

/** Projections for every field in a complete market-data snapshot. */
@ProjectionSchema
public interface MarketDataSnapshotProjection {

    /**
     * Returns the capture timestamp.
     *
     * @return capture time in nanoseconds since the Unix epoch
     */
    long capturedAtNanos();

    /**
     * Returns the shared market symbol.
     *
     * @return market symbol
     */
    String symbol();

    /**
     * Returns the price of the completed trade.
     *
     * @return last trade price
     */
    double lastTradePrice();

    /**
     * Returns the size of the completed trade.
     *
     * @return last trade size
     */
    double lastTradeSize();

    /**
     * Returns the best bid price.
     *
     * @return best bid price
     */
    double bidPrice();

    /**
     * Returns the best ask price.
     *
     * @return best ask price
     */
    double askPrice();

    /**
     * Returns the size available at the best bid.
     *
     * @return best bid size
     */
    double bidSize();

    /**
     * Returns the size available at the best ask.
     *
     * @return best ask size
     */
    double askSize();
}
