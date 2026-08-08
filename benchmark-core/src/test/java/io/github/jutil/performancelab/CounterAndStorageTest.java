package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CounterAndStorageTest {

    @Test
    void countersMergeMinMaxSumCountAndCalculateMeanFromSumAndCount() {
        Counter first = new Counter();
        first.add(-4.5);
        first.add(7.5);

        Counter second = new Counter();
        second.add(1.5);
        second.add(3.5);
        first.merge(second);

        assertEquals(-4.5, first.min());
        assertEquals(7.5, first.max());
        assertEquals(8.0, first.sum());
        assertEquals(4L, first.count());
        assertEquals(2.0, first.mean());
    }

    @Test
    void storageUsesStationKeysAndTracksMergedRowCount() {
        Storage first = new Storage();
        first.store(new Item("Yerevan", -2.0));
        first.store(new Item("Yerevan", 4.0));

        Storage second = new Storage();
        second.store(new Item("Berlin", 8.0));
        first.merge(second);

        assertEquals(3L, first.totalProcessedRowCount());
        assertEquals(2, first.counters().size());
        assertEquals(2L, first.counters().get("Yerevan").count());
        assertEquals(1L, first.counters().get("Berlin").count());
    }
}
