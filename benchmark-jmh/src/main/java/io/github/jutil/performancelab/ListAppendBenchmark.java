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

/** Fresh-list ordinary-add loops plus a separate optimized SpliceList endpoint loop. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@Threads(1)
public class ListAppendBenchmark {

    @Benchmark
    public ArrayList<Object> arrayListAdd(BenchmarkState state) {
        return ListAppendWorkload.arrayListAdd(state.elementCount, state.marker);
    }

    /** ArrayList's known-size best case: initial capacity equals the final size. */
    @Benchmark
    public ArrayList<Object> arrayListExactCapacityAdd(BenchmarkState state) {
        return ListAppendWorkload.arrayListExactCapacityAdd(
                state.elementCount,
                state.marker);
    }

    @Benchmark
    public SpliceList<Object> spliceListAdd(SegmentSizeState state) {
        return ListAppendWorkload.spliceListAdd(
                state.elementCount,
                state.segmentSize,
                state.marker);
    }

    /** Explicit optimized-endpoint counterpart to ordinary List.add. */
    @Benchmark
    public SpliceList<Object> spliceListAddLast(SegmentSizeState state) {
        return ListAppendWorkload.spliceListAddLast(
                state.elementCount,
                state.segmentSize,
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

    /** State shared by the ordinary and optimized SpliceList append methods. */
    @State(Scope.Benchmark)
    public static class SegmentSizeState extends BenchmarkState {

        @Param({"256", "1024", "4096", "10000", "20000", "30000"})
        public int segmentSize;
    }
}
