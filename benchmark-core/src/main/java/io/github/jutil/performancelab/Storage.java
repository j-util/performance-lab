package io.github.jutil.performancelab;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Local station aggregation used by every processor benchmark variant. */
public final class Storage {

    private final HashMap<String, Counter> counters = new HashMap<>();
    private long totalProcessedRowCount;

    /** Stores one parsed measurement. */
    public void store(Item item) {
        counters.computeIfAbsent(item.key(), ignored -> new Counter()).add(item.value());
        totalProcessedRowCount++;
    }

    /** Merges another local aggregation into this aggregation. */
    public void merge(Storage other) {
        other.counters.forEach((key, counter) ->
                counters.computeIfAbsent(key, ignored -> new Counter()).merge(counter));
        totalProcessedRowCount = Math.addExact(
                totalProcessedRowCount,
                other.totalProcessedRowCount);
    }

    /** Returns a read-only view used by correctness validation. */
    public Map<String, Counter> counters() {
        return Collections.unmodifiableMap(counters);
    }

    /** Returns the total number of measurements stored or merged. */
    public long totalProcessedRowCount() {
        return totalProcessedRowCount;
    }
}
