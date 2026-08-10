package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionSchema;
import io.github.jutil.columnarprojection.hardwood.HardwoodProjection;

/** Flat market-data projection shared by Hardwood and Columnar Projection Store. */
@ProjectionSchema
@HardwoodProjection
public interface HardwoodMarketDataProjection {

    long timestamp();

    String symbol();

    String venue();

    String side();

    long sequenceNumber();

    double bidPrice();

    double askPrice();

    double lastTradePrice();
}
