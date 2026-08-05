package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionSchema;

/** Timestamp and price projections for the complete price-tick logical record. */
@ProjectionSchema
public interface PriceTickProjection {

    /**
     * Returns the event timestamp.
     *
     * @return event time as milliseconds since the Unix epoch
     */
    long timestamp();

    /**
     * Returns the recorded price.
     *
     * @return price value
     */
    double price();
}
