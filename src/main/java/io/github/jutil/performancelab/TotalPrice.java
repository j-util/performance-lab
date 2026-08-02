package io.github.jutil.performancelab;

import io.github.jutil.reductionstore.LongReducer;
import io.github.jutil.reductionstore.LongReduction;

import java.util.function.LongSupplier;

final class TotalPrice implements LongReduction<BenchmarkRow> {
    
    @Override
    public LongSupplier supplier() {
        return () -> 0L;
    }

    @Override
    public LongReducer<BenchmarkRow> reducer() {
        return (sum, row) -> sum + row.priceCents();
    }
}
