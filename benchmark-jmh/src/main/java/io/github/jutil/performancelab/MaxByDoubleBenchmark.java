package io.github.jutil.performancelab;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.performancelab.MaxByDoubleStateSupport.ArrayListState;
import io.github.jutil.performancelab.MaxByDoubleStateSupport.ColumnarProjectionStoreState;
import io.github.jutil.performancelab.MaxByDoubleStateSupport.EclipseFastListState;
import io.github.jutil.performancelab.MaxByDoubleStateSupport.ManualHybridState;

/** Eclipse Collections JMH-derived maximum-by-double ready-data workload. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MaxByDoubleBenchmark {

    @Benchmark
    public Position arrayListImperativeMaxByDouble(ArrayListState state) {
        return MaxByDoubleCases.arrayListImperativeMaxByDouble(state.positions);
    }

    @Benchmark
    public Position arrayListStreamMaxByDouble(ArrayListState state) {
        return MaxByDoubleCases.arrayListStreamMaxByDouble(state.positions);
    }

    @Benchmark
    public Position eclipseFastListMaxByDouble(EclipseFastListState state) {
        return MaxByDoubleCases.eclipseFastListMaxByDouble(state.positions);
    }

    @Benchmark
    public Position columnarProjectionStoreMaxByDouble(ColumnarProjectionStoreState state) {
        return MaxByDoubleCases.columnarProjectionStoreMaxByDouble(state.store);
    }

    @Benchmark
    public Position manualHybridMaxByDouble(ManualHybridState state) {
        return MaxByDoubleCases.manualHybridMaxByDouble(state.hybrid);
    }
}
