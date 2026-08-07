package io.github.jutil.performancelab;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.columnarprojection.ProjectionStore;

/** Traversal ergonomics and efficiency for a sealed Columnar Projection Store. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ColumnarProjectionStoreIterationBenchmark {

    @Benchmark
    public double cursorLastTradePriceSum(StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.cursorLastTradePriceSum(
                storeState.store, accumulatorState.lastTradePriceSum);
    }

    @Benchmark
    public double indexedStableViewLastTradePriceSum(
            StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.indexedStableViewLastTradePriceSum(
                storeState.store, accumulatorState.lastTradePriceSum);
    }

    // TODO: Restore after columnar-projection-store:1.2.0 is published.
    /*
    @Benchmark
    public double forEachLastTradePriceSum(
            StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.forEachLastTradePriceSum(
                storeState.store, accumulatorState.lastTradePriceSum);
    }
    */

    @Benchmark
    public long cursorFullRowChecksum(StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.cursorFullRowChecksum(
                storeState.store, accumulatorState.fullRowChecksum);
    }

    @Benchmark
    public long indexedStableViewFullRowChecksum(
            StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.indexedStableViewFullRowChecksum(
                storeState.store, accumulatorState.fullRowChecksum);
    }

    /*
    @Benchmark
    public long forEachFullRowChecksum(
            StoreState storeState, AccumulatorState accumulatorState) {
        return ColumnarProjectionStoreIterationCases.forEachFullRowChecksum(
                storeState.store, accumulatorState.fullRowChecksum);
    }
    */

    /** Sealed immutable store shared by all benchmark threads. */
    @State(Scope.Benchmark)
    public static class StoreState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ProjectionStore<MarketDataSnapshotProjection> store;

        @Setup(Level.Trial)
        public void setup() {
            store = ColumnarProjectionStoreIterationCases.newStore(rowCount);
            ColumnarProjectionStoreIterationCases.validate(store, rowCount);
        }
    }

    /** Resettable accumulators private to each benchmark thread. */
    @State(Scope.Thread)
    public static class AccumulatorState {

        final ColumnarProjectionStoreIterationCases.LastTradePriceSumAccumulator
                lastTradePriceSum =
                        new ColumnarProjectionStoreIterationCases.LastTradePriceSumAccumulator();
        final ColumnarProjectionStoreIterationCases.FullRowChecksumAccumulator
                fullRowChecksum =
                        new ColumnarProjectionStoreIterationCases.FullRowChecksumAccumulator();
    }
}
