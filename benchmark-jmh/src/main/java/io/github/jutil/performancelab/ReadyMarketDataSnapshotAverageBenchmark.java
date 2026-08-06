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
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.ApacheArrowColumnarState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.ChronicleValuesBytesRowState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.ColumnarProjectionStoreState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.DoubleArrayBaselineState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.DflibDataFrameState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.EclipseFastListState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.FastUtilObjectArrayListState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.HppcColumnarState;
import io.github.jutil.performancelab.ReadyMarketDataSnapshotAverageStateSupport.MemorySegmentRowState;
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
    public double dflibDataFrameLastTradePriceAverage(DflibDataFrameState state) {
        return ReadyMarketDataSnapshotAverageCases
                .dflibDataFrameLastTradePriceAverage(state.dataFrame);
    }

    @Benchmark
    public double dflibDataFrameNaiveLastTradePriceAverage(DflibDataFrameState state) {
        return ReadyMarketDataSnapshotAverageCases
                .dflibDataFrameNaiveLastTradePriceAverage(state.dataFrame);
    }

    @Benchmark
    public double hppcColumnarLastTradePriceAverage(HppcColumnarState state) {
        return ReadyMarketDataSnapshotAverageCases
                .hppcColumnarLastTradePriceAverage(state.columns);
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

    @Benchmark
    public double memorySegmentRowLastTradePriceAverage(MemorySegmentRowState state) {
        return ReadyMarketDataSnapshotAverageCases
                .memorySegmentRowLastTradePriceAverage(state.store);
    }

    @Benchmark
    @Fork(
            value = 1,
            jvmArgsAppend = {
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow"
            })
    public double chronicleValuesBytesRowLastTradePriceAverage(
            ChronicleValuesBytesRowState state) {
        return ReadyMarketDataSnapshotAverageCases
                .chronicleValuesBytesRowLastTradePriceAverage(state.store);
    }

    @Benchmark
    @Fork(
            value = 1,
            jvmArgsAppend = {
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "--sun-misc-unsafe-memory-access=allow"
            })
    public double apacheArrowColumnarLastTradePriceAverage(ApacheArrowColumnarState state) {
        return ReadyMarketDataSnapshotAverageCases
                .apacheArrowColumnarLastTradePriceAverage(state.store);
    }
}
