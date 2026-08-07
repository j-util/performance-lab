package io.github.jutil.performancelab;

/** A product shared by benchmark positions. */
public record Product(int id, double price) {

    public Product {
        if (id < 0) {
            throw new IllegalArgumentException("Product ID must not be negative: " + id);
        }
        if (!Double.isFinite(price) || price <= 0.0d) {
            throw new IllegalArgumentException("Product price must be finite and positive: " + price);
        }
    }
}
