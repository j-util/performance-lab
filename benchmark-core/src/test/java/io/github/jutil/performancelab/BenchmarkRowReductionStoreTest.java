package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BenchmarkRowReductionStoreTest {

    @Test
    void generatedStoreExecutesAllBenchmarkRowReductions() {
        BenchmarkRowReductionStore store = new BenchmarkRowReductionStore();

        store.add(new BenchmarkRow(1, 1, 1, 4, 100, 1, "region", "status"));
        store.add(new BenchmarkRow(2, 1, 1, 5, 200, 2, "region", "status"));
        store.add(new BenchmarkRow(3, 1, 1, 6, 300, 3, "region", "status"));

        assertEquals(600, store.totalPrice());
        assertEquals(500, store.filteredPriceSum());
    }
}
