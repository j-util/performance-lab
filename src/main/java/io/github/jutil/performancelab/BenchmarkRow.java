package io.github.jutil.performancelab;

/**
 * A single deterministic input row for performance experiments.
 *
 * @param id unique row identifier
 * @param customerId customer identifier
 * @param productId product identifier
 * @param quantity purchased quantity
 * @param priceCents unit price in cents
 * @param timestamp event time as milliseconds since the Unix epoch
 * @param region sales region
 * @param status order status
 */
public record BenchmarkRow(
        long id,
        long customerId,
        int productId,
        int quantity,
        long priceCents,
        long timestamp,
        String region,
        String status) implements BenchmarkProjection {
}
