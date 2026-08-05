package io.github.jutil.performancelab;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.ArrayListState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.ColumnarProjectionStoreState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.DoubleArrayBaselineState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.EclipseFastListState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.FastUtilObjectArrayListState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.TablesawTableState;

/** Last-trade-price averages over ready, complete market-data snapshots. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ReadyMarketDataSnapshotAverageBenchmark {

    @Benchmark
    public double arrayListLastTradePriceAverage(ArrayListState state) {
        return ReadyMarketDataSnapshotAverageCases.arrayListLastTradePriceAverage(state.rows);
    }

    @Benchmark
    public double fastUtilObjectArrayListLastTradePriceAverage(
            FastUtilObjectArrayListState state) {
        return ReadyMarketDataSnapshotAverageCases
                .fastUtilObjectArrayListLastTradePriceAverage(state.rows);
    }

    @Benchmark
    public double eclipseFastListLastTradePriceAverage(EclipseFastListState state) {
        return ReadyMarketDataSnapshotAverageCases.eclipseFastListLastTradePriceAverage(state.rows);
    }

    @Benchmark
    public double eclipseFastListNaiveLastTradePriceAverage(EclipseFastListState state) {
        return ReadyMarketDataSnapshotAverageCases
                .eclipseFastListNaiveLastTradePriceAverage(state.rows);
    }

    @Benchmark
    public double tablesawTableLastTradePriceAverage(TablesawTableState state) {
        return ReadyMarketDataSnapshotAverageCases.tablesawTableLastTradePriceAverage(state.table);
    }

    @Benchmark
    public double tablesawTableNaiveLastTradePriceAverage(TablesawTableState state) {
        return ReadyMarketDataSnapshotAverageCases
                .tablesawTableNaiveLastTradePriceAverage(state.table);
    }

    @Benchmark
    public double columnarProjectionStoreLastTradePriceAverage(
            ColumnarProjectionStoreState state) {
        return ReadyMarketDataSnapshotAverageCases
                .columnarProjectionStoreLastTradePriceAverage(state.store);
    }

    @Benchmark
    public double doubleArrayBaselineLastTradePriceAverage(DoubleArrayBaselineState state) {
        return ReadyMarketDataSnapshotAverageCases
                .doubleArrayBaselineLastTradePriceAverage(state.lastTradePrices);
    }
}
