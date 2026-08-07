package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionSchema;

/** Primitive market value and original-object projections for a position. */
@ProjectionSchema
public interface PositionProjection {

    /**
     * Returns the position's market value.
     *
     * @return quantity multiplied by product price
     */
    double marketValue();

    /**
     * Returns the original position supplied to the projection store.
     *
     * @return the original position
     */
    Position original();
}
