package io.github.jutil.performancelab;

import java.util.function.Consumer;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;

/** Workloads shared by the Columnar Projection Store iteration benchmarks and tests. */
final class ColumnarProjectionStoreIterationCases {

    private ColumnarProjectionStoreIterationCases() {
    }

    static ProjectionStore<MarketDataSnapshotProjection> newStore(int rowCount) {
        return ReadyMarketDataSnapshotAverageCases.newColumnarProjectionStore(rowCount);
    }

    static double cursorLastTradePriceSum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            LastTradePriceSumAccumulator accumulator) {
        accumulator.reset();
        ProjectionCursor<MarketDataSnapshotProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            accumulator.accept(cursor.current());
        }
        return accumulator.result();
    }

    static double indexedStableViewLastTradePriceSum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            LastTradePriceSumAccumulator accumulator) {
        accumulator.reset();
        for (int index = 0, size = store.size(); index < size; index++) {
            accumulator.accept(store.viewAt(index));
        }
        return accumulator.result();
    }

    // TODO: Restore after columnar-projection-store:1.2.0 is published.
    /*
    static double forEachLastTradePriceSum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            LastTradePriceSumAccumulator accumulator) {
        accumulator.reset();
        store.forEach(accumulator);
        return accumulator.result();
    }
    */

    static long cursorFullRowChecksum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            FullRowChecksumAccumulator accumulator) {
        accumulator.reset();
        ProjectionCursor<MarketDataSnapshotProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            accumulator.accept(cursor.current());
        }
        return accumulator.result();
    }

    static long indexedStableViewFullRowChecksum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            FullRowChecksumAccumulator accumulator) {
        accumulator.reset();
        for (int index = 0, size = store.size(); index < size; index++) {
            accumulator.accept(store.viewAt(index));
        }
        return accumulator.result();
    }

    /*
    static long forEachFullRowChecksum(
            ProjectionStore<MarketDataSnapshotProjection> store,
            FullRowChecksumAccumulator accumulator) {
        accumulator.reset();
        store.forEach(accumulator);
        return accumulator.result();
    }
    */

    static void validate(ProjectionStore<MarketDataSnapshotProjection> store, int rowCount) {
        if (store.size() != rowCount) {
            throw new IllegalStateException(
                    "Columnar Projection Store contains " + store.size()
                            + " rows; expected " + rowCount);
        }

        LastTradePriceSumAccumulator sumAccumulator = new LastTradePriceSumAccumulator();
        double cursorSum = cursorLastTradePriceSum(store, sumAccumulator);
        validateVisited("cursor last-trade-price sum", rowCount, sumAccumulator.count());
        double indexedSum = indexedStableViewLastTradePriceSum(store, sumAccumulator);
        validateVisited("indexed stable-view last-trade-price sum", rowCount, sumAccumulator.count());
        /*
        double forEachSum = forEachLastTradePriceSum(store, sumAccumulator);
        validateVisited("forEach last-trade-price sum", rowCount, sumAccumulator.count());
        */
        if (Double.doubleToLongBits(cursorSum) != Double.doubleToLongBits(indexedSum)) {
            throw new IllegalStateException(
                    "Last-trade-price traversal results differ: cursor=" + cursorSum
                            + ", indexed=" + indexedSum);
        }

        FullRowChecksumAccumulator checksumAccumulator = new FullRowChecksumAccumulator();
        long cursorChecksum = cursorFullRowChecksum(store, checksumAccumulator);
        validateVisited("cursor full-row checksum", rowCount, checksumAccumulator.count());
        long indexedChecksum = indexedStableViewFullRowChecksum(store, checksumAccumulator);
        validateVisited("indexed stable-view full-row checksum", rowCount, checksumAccumulator.count());
        /*
        long forEachChecksum = forEachFullRowChecksum(store, checksumAccumulator);
        validateVisited("forEach full-row checksum", rowCount, checksumAccumulator.count());
        */
        if (cursorChecksum != indexedChecksum) {
            throw new IllegalStateException(
                    "Full-row traversal results differ: cursor=" + cursorChecksum
                            + ", indexed=" + indexedChecksum);
        }
    }

    private static void validateVisited(String operation, int expected, int actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    operation + " visited " + actual + " rows; expected " + expected);
        }
    }

    static final class LastTradePriceSumAccumulator
            implements Consumer<MarketDataSnapshotProjection> {

        private double sum;
        private int count;

        @Override
        public void accept(MarketDataSnapshotProjection row) {
            sum += row.lastTradePrice();
            count++;
        }

        void reset() {
            sum = 0.0d;
            count = 0;
        }

        double result() {
            return sum;
        }

        int count() {
            return count;
        }
    }

    static final class FullRowChecksumAccumulator
            implements Consumer<MarketDataSnapshotProjection> {

        private static final long INITIAL_CHECKSUM = 0x6A09E667F3BCC909L;
        private static final long MIX_MULTIPLIER = 0x9E3779B185EBCA87L;

        private long checksum;
        private int count;

        FullRowChecksumAccumulator() {
            reset();
        }

        @Override
        public void accept(MarketDataSnapshotProjection row) {
            checksum = mix(checksum, row.capturedAtNanos());
            checksum = mix(checksum, row.symbol().hashCode());
            checksum = mix(checksum, Double.doubleToLongBits(row.lastTradePrice()));
            checksum = mix(checksum, Double.doubleToLongBits(row.lastTradeSize()));
            checksum = mix(checksum, Double.doubleToLongBits(row.bidPrice()));
            checksum = mix(checksum, Double.doubleToLongBits(row.askPrice()));
            checksum = mix(checksum, Double.doubleToLongBits(row.bidSize()));
            checksum = mix(checksum, Double.doubleToLongBits(row.askSize()));
            count++;
        }

        void reset() {
            checksum = INITIAL_CHECKSUM;
            count = 0;
        }

        long result() {
            return checksum;
        }

        int count() {
            return count;
        }

        private static long mix(long current, long value) {
            long mixed = current ^ Long.rotateLeft(value * MIX_MULTIPLIER, 27);
            return Long.rotateLeft(mixed, 31) * MIX_MULTIPLIER;
        }
    }
}
