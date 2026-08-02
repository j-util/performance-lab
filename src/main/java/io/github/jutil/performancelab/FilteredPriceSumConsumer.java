package io.github.jutil.performancelab;

import java.util.function.Consumer;

/** Handwritten primitive streaming reduction used as the benchmark baseline. */
final class FilteredPriceSumConsumer implements Consumer<BenchmarkRow> {

    private long sum;

    @Override
    public void accept(BenchmarkRow row) {
        if (row.quantity() >= 5) {
            sum += row.priceCents();
        }
    }

    long sum() {
        return sum;
    }

}
