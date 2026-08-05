package io.github.jutil.performancelab;

/**
 * A complete logical record used by the ready-data price-average benchmarks.
 *
 * @param timestamp event time as milliseconds since the Unix epoch
 * @param price recorded price
 */
public record PriceTick(long timestamp, double price) implements PriceTickProjection {
}
