package io.github.jutil.performancelab;

import java.util.function.Consumer;

/** Deterministic allocation-free checksum over every full-row field. */
final class FullRowChecksumConsumer implements Consumer<BenchmarkProjection> {

    private long checksum;
    private long count;

    @Override
    public void accept(BenchmarkProjection row) {
        long mixed = row.id();
        mixed = mix(mixed, row.customerId());
        mixed = mix(mixed, row.productId());
        mixed = mix(mixed, row.quantity());
        mixed = mix(mixed, row.priceCents());
        mixed = mix(mixed, row.timestamp());
        mixed = mix(mixed, row.region().hashCode());
        mixed = mix(mixed, row.status().hashCode());
        checksum = mix(checksum, mixed);
        count++;
    }

    long checksum() {
        return checksum;
    }

    long count() {
        return count;
    }

    private static long mix(long left, long right) {
        long value = left ^ (right + 0x9E3779B97F4A7C15L + (left << 6) + (left >>> 2));
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
