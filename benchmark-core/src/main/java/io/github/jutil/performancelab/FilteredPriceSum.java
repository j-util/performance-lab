package io.github.jutil.performancelab;

import java.util.function.LongSupplier;

import io.github.jutil.reductionstore.LongReducer;
import io.github.jutil.reductionstore.LongReduction;

/** Reduction-store implementation of the filtered primitive price sum. */
final class FilteredPriceSum implements LongReduction<BenchmarkRow> {

    @Override
    public LongSupplier supplier() {
        return () -> 0L;
    }

    @Override
    public LongReducer<BenchmarkRow> reducer() {
        return (sum, row) -> row.quantity() >= 5
                ? sum + row.priceCents()
                : sum;
    }
}
