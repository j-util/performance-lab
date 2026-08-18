package io.github.jutil.performancelab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.splicelist.SpliceList;

/** Parallel worker-local list filling followed by deterministic consolidation. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@Threads(1)
public class ParallelListFillAndCombineBenchmark {

    @Benchmark
    public ArrayList<Object> arrayListAddAll(BenchmarkState state)
            throws InterruptedException, ExecutionException {
        return ParallelListFillAndCombineWorkload.arrayListAddAll(
                state.elements,
                state.parallelism,
                state.executor);
    }

    @Benchmark
    public SpliceList<Object> spliceListSpliceTail(BenchmarkState state)
            throws InterruptedException, ExecutionException {
        return ParallelListFillAndCombineWorkload.spliceListSpliceTail(
                state.elements,
                state.parallelism,
                state.executor);
    }

    @Benchmark
    public ArrayList<Object> arrayListAddAllMergeOnly(ArrayListMergeOnlyState state) {
        return ParallelListFillAndCombineWorkload.combineArrayLists(
                state.arrayListPartials,
                state.elementCount);
    }

    @Benchmark
    public SpliceList<Object> spliceListSpliceTailMergeOnly(SpliceListMergeOnlyState state) {
        return ParallelListFillAndCombineWorkload.combineSpliceLists(
                state.spliceListPartials);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"10000"})
        public int elementCount;

        @Param({"8"})
        public int parallelism;

        Object[] elements;
        ExecutorService executor;

        @Setup(Level.Trial)
        public void setup() {
            if (elementCount < 0) {
                throw new IllegalArgumentException(
                        "elementCount must be non-negative: " + elementCount);
            }
            if (parallelism <= 0) {
                throw new IllegalArgumentException(
                        "parallelism must be positive: " + parallelism);
            }

            Object marker = new Object();
            elements = new Object[elementCount];
            Arrays.fill(elements, marker);
            executor = Executors.newFixedThreadPool(parallelism);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws InterruptedException {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Benchmark executor did not terminate");
                }
            }
        }
    }

    /** Recreates prepared ArrayList partitions before each merge-only iteration. */
    @State(Scope.Thread)
    public static class ArrayListMergeOnlyState {

        @Param({"10000"})
        public int elementCount;

        @Param({"8"})
        public int parallelism;

        Object[] elements;
        List<ArrayList<Object>> arrayListPartials;

        @Setup(Level.Trial)
        public void setupTrial() {
            if (elementCount < 0) {
                throw new IllegalArgumentException(
                        "elementCount must be non-negative: " + elementCount);
            }
            if (parallelism <= 0) {
                throw new IllegalArgumentException(
                        "parallelism must be positive: " + parallelism);
            }

            Object marker = new Object();
            elements = new Object[elementCount];
            Arrays.fill(elements, marker);
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            arrayListPartials = ParallelListFillAndCombineWorkload
                    .prepareArrayListPartials(elements, parallelism);
        }
    }

    /** Recreates prepared SpliceList partitions before each merge-only iteration. */
    @State(Scope.Thread)
    public static class SpliceListMergeOnlyState {

        @Param({"10000"})
        public int elementCount;

        @Param({"8"})
        public int parallelism;

        Object[] elements;
        List<SpliceList<Object>> spliceListPartials;

        @Setup(Level.Trial)
        public void setupTrial() {
            if (elementCount < 0) {
                throw new IllegalArgumentException(
                        "elementCount must be non-negative: " + elementCount);
            }
            if (parallelism <= 0) {
                throw new IllegalArgumentException(
                        "parallelism must be positive: " + parallelism);
            }

            Object marker = new Object();
            elements = new Object[elementCount];
            Arrays.fill(elements, marker);
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            spliceListPartials = ParallelListFillAndCombineWorkload
                    .prepareSpliceListPartials(elements, parallelism);
        }
    }
}
