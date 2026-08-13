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

/** Sequential iterator traversal over already-populated collection representations. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@Threads(1)
public class ReadyCollectionIterationBenchmark {

    @Benchmark
    public double arrayListIterator(ArrayListState state) {
        return ReadyCollectionIterationCases.iteratorSum(state.items);
    }

    @Benchmark
    public double oneSegmentSpliceListIterator(OneSegmentSpliceListState state) {
        return ReadyCollectionIterationCases.iteratorSum(state.items);
    }

    @Benchmark
    public double tenSegmentSpliceListIterator(TenSegmentSpliceListState state) {
        return ReadyCollectionIterationCases.iteratorSum(state.items);
    }

    /** Retains only the exactly pre-sized ArrayList representation. */
    @State(Scope.Benchmark)
    public static class ArrayListState {

        @Param({"10000000"})
        public int rowCount;

        ArrayList<Item> items;

        @Setup(Level.Trial)
        public void setup() {
            items = ReadyCollectionIterationCases.newArrayList(rowCount);
            ReadyCollectionIterationCases.validateFixture("ArrayList", rowCount, items);
        }
    }

    /** Retains only the SpliceList whose regular segment capacity equals rowCount. */
    @State(Scope.Benchmark)
    public static class OneSegmentSpliceListState {

        @Param({"10000000"})
        public int rowCount;

        SpliceList<Item> items;

        @Setup(Level.Trial)
        public void setup() {
            items = ReadyCollectionIterationCases.newOneSegmentSpliceList(rowCount);
            ReadyCollectionIterationCases.validateFixture(
                    "one-segment SpliceList", rowCount, items);
        }
    }

    /**
     * Retains only the SpliceList with regular segment capacity ceil(rowCount / 10).
     * At the default row count this is exactly ten full 1,000,000-element segments because
     * 10,000,000 is divisible by ten.
     */
    @State(Scope.Benchmark)
    public static class TenSegmentSpliceListState {

        @Param({"10000000"})
        public int rowCount;

        SpliceList<Item> items;

        @Setup(Level.Trial)
        public void setup() {
            items = ReadyCollectionIterationCases.newTenSegmentSpliceList(rowCount);
            ReadyCollectionIterationCases.validateFixture(
                    "ten-segment SpliceList", rowCount, items);
        }
    }
}
