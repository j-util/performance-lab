package io.github.jutil.performancelab;

import java.util.ArrayList;
import java.util.Objects;

import io.github.jutil.splicelist.SpliceList;

/** Construction, iterator traversal, and validation for ready collection iteration cases. */
final class ReadyCollectionIterationCases {

    private static final String ITEM_KEY = "ready-collection-item";
    private static final int TEN_SEGMENTS = 10;

    private ReadyCollectionIterationCases() {
    }

    static ArrayList<IterationItem> newArrayList(int rowCount) {
        validateRowCount(rowCount);
        ArrayList<IterationItem> items = new ArrayList<>(rowCount);
        addItems(rowCount, items::add);
        return items;
    }

    static SpliceList<IterationItem> newOneSegmentSpliceList(int rowCount) {
        validateRowCount(rowCount);
        SpliceList<IterationItem> items = new SpliceList<>(rowCount);
        addItems(rowCount, items::addLast);
        return items;
    }

    /**
     * Creates a SpliceList whose regular segment capacity is {@code ceil(rowCount / 10)}.
     * The default 10,000,000-row input therefore occupies exactly ten full regular segments.
     */
    static SpliceList<IterationItem> newTenSegmentSpliceList(int rowCount) {
        SpliceList<IterationItem> items = new SpliceList<>(tenSegmentSize(rowCount));
        addItems(rowCount, items::addLast);
        return items;
    }

    static int tenSegmentSize(int rowCount) {
        validateRowCount(rowCount);
        return Math.ceilDiv(rowCount, TEN_SEGMENTS);
    }

    /** Performs exactly one enhanced-for traversal and returns its sum. */
    static double iteratorSum(Iterable<IterationItem> items) {
        Objects.requireNonNull(items, "items");
        double sum = 0.0;
        for (IterationItem item : items) {
            sum += item.value();
        }
        return sum;
    }

    /**
     * Validates fixture values, encounter order, row count, and the exact iterator sum.
     * Each representation-specific trial setup compares with the same canonical sum, which
     * establishes exact agreement without retaining multiple large representations together.
     */
    static void validateFixture(String representation, int rowCount, Iterable<IterationItem> items) {
        Objects.requireNonNull(representation, "representation");
        Objects.requireNonNull(items, "items");
        validateRowCount(rowCount);

        int index = 0;
        for (IterationItem item : items) {
            if (index >= rowCount) {
                throw new IllegalStateException(
                        representation + " contains more than " + rowCount + " items");
            }
            if (!ITEM_KEY.equals(item.key())
                    || Double.compare(item.value(), valueAt(index)) != 0) {
                throw new IllegalStateException(
                        representation + " differs from the fixture at index " + index);
            }
            index++;
        }
        if (index != rowCount) {
            throw new IllegalStateException(
                    representation + " contains " + index + " items; expected " + rowCount);
        }

        double actualSum = iteratorSum(items);
        double expectedSum = expectedSum(rowCount);
        if (!Double.isFinite(actualSum)
                || Double.doubleToLongBits(actualSum) != Double.doubleToLongBits(expectedSum)) {
            throw new IllegalStateException(
                    representation + " sum was " + actualSum + "; expected " + expectedSum);
        }
    }

    static IterationItem itemAt(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be non-negative: " + index);
        }
        return new IterationItem(ITEM_KEY, valueAt(index));
    }

    private static void addItems(int rowCount, java.util.function.Consumer<IterationItem> destination) {
        for (int index = 0; index < rowCount; index++) {
            destination.accept(itemAt(index));
        }
    }

    private static double expectedSum(int rowCount) {
        double sum = 0.0;
        for (int index = 0; index < rowCount; index++) {
            sum += valueAt(index);
        }
        return sum;
    }

    private static double valueAt(int index) {
        return index;
    }

    private static void validateRowCount(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("rowCount must be positive: " + rowCount);
        }
    }
}
