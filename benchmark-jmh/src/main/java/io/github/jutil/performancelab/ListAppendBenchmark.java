package io.github.jutil.performancelab;

import java.util.ArrayList;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.splicelist.SpliceList;

/** Fresh-list matched ordinary-add loops plus explicitly contextual baselines. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@Threads(1)
public class ListAppendBenchmark {

    @Benchmark
    public ArrayList<Object> matchedArrayListOrdinaryAdd(CapacityHintState state) {
        return ListAppendWorkload.arrayListCapacityHintAdd(
                state.elementCount,
                state.capacityHint,
                state.marker);
    }

    @Benchmark
    public SpliceList<Object> matchedSpliceListOrdinaryAdd(CapacityHintState state) {
        return ListAppendWorkload.spliceListSegmentSizeAdd(
                state.elementCount,
                state.capacityHint,
                state.marker);
    }

    /** Context only: ArrayList grows from its default initial state. */
    @Benchmark
    public ArrayList<Object> contextArrayListDefaultGrowing(BenchmarkState state) {
        return ListAppendWorkload.arrayListDefaultGrowingAdd(
                state.elementCount,
                state.marker);
    }

    /** Context only: ArrayList's initial capacity equals the final size. */
    @Benchmark
    public ArrayList<Object> contextArrayListExactFinalCapacity(BenchmarkState state) {
        return ListAppendWorkload.arrayListExactFinalCapacityAdd(
                state.elementCount,
                state.marker);
    }

    /** Context only: SpliceList uses its optimized endpoint instead of ordinary List.add. */
    @Benchmark
    public SpliceList<Object> contextSpliceListOptimizedAddLast(CapacityHintState state) {
        return ListAppendWorkload.spliceListSegmentSizeAddLast(
                state.elementCount,
                state.capacityHint,
                state.marker);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"10000"})
        public int elementCount;

        Object marker;

        @Setup(Level.Trial)
        public void setup() {
            if (elementCount < 0) {
                throw new IllegalArgumentException(
                        "elementCount must be non-negative: " + elementCount);
            }
            marker = new Object();
        }
    }

    /** Shared constructor argument for the matched pair and optimized SpliceList context. */
    @State(Scope.Benchmark)
    public static class CapacityHintState extends BenchmarkState {

        @Param({"256", "1024", "4096", "10000", "20000", "30000"})
        public int capacityHint;
    }
}
