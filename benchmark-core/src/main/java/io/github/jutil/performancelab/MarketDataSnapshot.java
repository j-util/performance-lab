package io.github.jutil.performancelab;

/**
 * Complete market state captured immediately after a distinct completed trade.
 *
 * @param capturedAtNanos capture time in nanoseconds since the Unix epoch
 * @param symbol shared market symbol
 * @param lastTradePrice price of the completed trade
 * @param lastTradeSize size of the completed trade
 * @param bidPrice best bid price
 * @param askPrice best ask price
 * @param bidSize size available at the best bid
 * @param askSize size available at the best ask
 */
public record MarketDataSnapshot(
        long capturedAtNanos,
        String symbol,
        double lastTradePrice,
        double lastTradeSize,
        double bidPrice,
        double askPrice,
        double bidSize,
        double askSize) implements MarketDataSnapshotProjection {
}
