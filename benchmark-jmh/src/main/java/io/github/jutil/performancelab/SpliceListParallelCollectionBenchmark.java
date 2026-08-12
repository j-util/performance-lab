package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.openjdk.jmh.annotations.Warmup;

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.parallelrangeprocessor.ParallelRangeProcessor;
import io.github.jutil.splicelist.SpliceList;

/** End-to-end comparison of sequential collection and parallel partial-list assembly. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
public class SpliceListParallelCollectionBenchmark {

    @Benchmark
    public ArrayList<Item> parallelArrayListAddAll(BenchmarkState state)
            throws IOException, InterruptedException {
        return SpliceListParallelCollectionWorkload.parallelArrayList(
                state.input,
                state.rowCount,
                state.arrayListProcessor);
    }

    @Benchmark
    public ArrayList<Item> sequentialArrayList(BenchmarkState state) throws IOException {
        return SpliceListParallelCollectionWorkload.sequentialArrayList(
                state.input,
                state.rowCount,
                state.sequentialProcessor);
    }

    @Benchmark
    public SpliceList<Item> parallelSpliceListSpliceTail(BenchmarkState state)
            throws IOException, InterruptedException {
        return SpliceListParallelCollectionWorkload.parallelSpliceList(
                state.input,
                state.spliceListProcessor);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"10000000"})
        public int rowCount;

        @Param({"8"})
        public int parallelism;

        Path input;
        ExecutorService executor;
        InputStreamProcessor<Item> sequentialProcessor;
        ParallelRangeProcessor<ArrayList<Item>> arrayListProcessor;
        ParallelRangeProcessor<SpliceList<Item>> spliceListProcessor;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            executor = Executors.newFixedThreadPool(parallelism);
            sequentialProcessor = SpliceListParallelCollectionWorkload.newSequentialProcessor();
            arrayListProcessor = SpliceListParallelCollectionWorkload.newArrayListProcessor(
                    parallelism,
                    executor);
            spliceListProcessor = SpliceListParallelCollectionWorkload.newSpliceListProcessor(
                    parallelism,
                    executor);
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

    private static Path requireDataset(int rowCount) {
        Path input = OneBrcStyleDatasetGenerator.defaultOutputPath(rowCount);
        if (!Files.isRegularFile(input)) {
            throw new IllegalStateException(
                    "1BRC-style benchmark dataset does not exist: "
                            + input.toAbsolutePath().normalize()
                            + ". Generate it first with OneBrcStyleDatasetGenerator "
                            + rowCount + ".");
        }
        return input;
    }
}
