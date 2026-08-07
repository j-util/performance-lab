package io.github.jutil.performancelab;

import java.util.Objects;

/**
 * A project-owned position used by the maximum-market-value workload.
 *
 * @param id deterministic row identifier
 * @param quantity units held
 * @param product referenced product supplying the price
 */
public record Position(long id, int quantity, Product product) implements PositionProjection {

    public Position {
        if (id < 0L) {
            throw new IllegalArgumentException("Position ID must not be negative: " + id);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Position quantity must be positive: " + quantity);
        }
        Objects.requireNonNull(product, "product");
    }

    @Override
    public double marketValue() {
        return quantity * product.price();
    }

    @Override
    public Position original() {
        return this;
    }
}
