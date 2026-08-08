package io.github.jutil.performancelab;

/** Mutable statistics for one station, owned by one local aggregation. */
public final class Counter {

    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private double sum;
    private long count;

    /** Adds one temperature to this counter. */
    public void add(double value) {
        min = Math.min(min, value);
        max = Math.max(max, value);
        sum += value;
        count++;
    }

    /** Merges another partial counter into this counter. */
    public void merge(Counter other) {
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
        sum += other.sum;
        count = Math.addExact(count, other.count);
    }

    /** Returns the arithmetic mean, calculated from the mergeable sum and count. */
    public double mean() {
        return sum / count;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double sum() {
        return sum;
    }

    public long count() {
        return count;
    }
}
