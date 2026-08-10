package io.github.jutil.performancelab;

/** Immutable object representation used by the Hardwood-to-ArrayList path. */
public record HardwoodMarketDataRow(
        long timestamp,
        String symbol,
        String venue,
        String side,
        long sequenceNumber,
        double bidPrice,
        double askPrice,
        double lastTradePrice)
        implements HardwoodMarketDataProjection {}
