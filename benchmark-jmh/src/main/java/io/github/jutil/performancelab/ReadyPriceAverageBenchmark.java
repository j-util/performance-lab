package io.github.jutil.performancelab;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.ArrayListState;
import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.ColumnarProjectionStoreState;
import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.EclipseFastListState;
import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.FastUtilObjectArrayListState;
import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.PrimitiveArraysState;
import io.github.jutil.performancelab.ReadyPriceAverageStateSupport.TablesawTableState;

/** Price-average benchmarks over already-materialized, representation-specific data. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ReadyPriceAverageBenchmark {

    @Benchmark
    public double arrayListPriceAverage(ArrayListState state) {
        return ReadyPriceAverageCases.arrayListPriceAverage(state.rows);
    }

    @Benchmark
    public double fastUtilObjectArrayListPriceAverage(FastUtilObjectArrayListState state) {
        return ReadyPriceAverageCases.fastUtilObjectArrayListPriceAverage(state.rows);
    }

    @Benchmark
    public double eclipseFastListPriceAverage(EclipseFastListState state) {
        return ReadyPriceAverageCases.eclipseFastListPriceAverage(state.rows);
    }

    @Benchmark
    public double tablesawTablePriceAverage(TablesawTableState state) {
        return ReadyPriceAverageCases.tablesawTablePriceAverage(state.table);
    }

    @Benchmark
    public double primitiveArraysPriceAverage(PrimitiveArraysState state) {
        return ReadyPriceAverageCases.primitiveArraysPriceAverage(state.prices);
    }

    @Benchmark
    public double columnarProjectionStorePriceAverage(ColumnarProjectionStoreState state) {
        return ReadyPriceAverageCases.columnarProjectionStorePriceAverage(state.store);
    }
}
