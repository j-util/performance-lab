package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
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

/** 1BRC-style processor benchmark using the same Commons CSV mapping and aggregation. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
public class OneBrcStyleProcessorBenchmark {

    @Benchmark
    public Storage filesLinesSequential(FilesLinesSequentialState state) throws IOException {
        return OneBrcStyleProcessorWorkload.filesLinesSequential(state.input);
    }

    @Benchmark
    public Storage filesLinesParallelForkJoinPool(FilesLinesParallelState state)
            throws InterruptedException, ExecutionException {
        return OneBrcStyleProcessorWorkload.filesLinesParallel(state.input, state.pool);
    }

    @Benchmark
    public Storage inputStreamProcessorCore(InputStreamProcessorState state) throws IOException {
        return OneBrcStyleProcessorWorkload.inputStreamProcessorCore(
                state.input,
                state.processor);
    }

    @Benchmark
    public Storage parallelRangeProcessorForkJoinPool(ParallelRangeForkJoinState state)
            throws IOException, InterruptedException {
        return OneBrcStyleProcessorWorkload.parallelRangeProcessor(
                state.input,
                state.processor);
    }

    @Benchmark
    public Storage parallelRangeProcessorFixedThreadPool(ParallelRangeFixedThreadPoolState state)
            throws IOException, InterruptedException {
        return OneBrcStyleProcessorWorkload.parallelRangeProcessor(
                state.input,
                state.processor);
    }

    @State(Scope.Benchmark)
    public static class FilesLinesSequentialState {

        @Param({"10000000"})
        public int rowCount;

        Path input;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
        }
    }

    @State(Scope.Benchmark)
    public static class FilesLinesParallelState {

        @Param({"10000000"})
        public int rowCount;

        @Param({"2", "4", "8"})
        public int parallelism;

        Path input;
        ForkJoinPool pool;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            pool = new ForkJoinPool(parallelism);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws InterruptedException {
            shutDown(pool);
        }
    }

    @State(Scope.Benchmark)
    public static class InputStreamProcessorState {

        @Param({"10000000"})
        public int rowCount;

        Path input;
        InputStreamProcessor<Storage> processor;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            processor = OneBrcStyleProcessorWorkload.newInputStreamProcessor();
        }
    }

    @State(Scope.Benchmark)
    public static class ParallelRangeForkJoinState {

        @Param({"10000000"})
        public int rowCount;

        @Param({"1", "2", "4", "8"})
        public int parallelism;

        Path input;
        ForkJoinPool pool;
        ParallelRangeProcessor<Storage> processor;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            pool = new ForkJoinPool(parallelism);
            processor = OneBrcStyleProcessorWorkload.newParallelRangeProcessor(
                    parallelism,
                    pool);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws InterruptedException {
            shutDown(pool);
        }
    }

    @State(Scope.Benchmark)
    public static class ParallelRangeFixedThreadPoolState {

        @Param({"10000000"})
        public int rowCount;

        @Param({"1", "2", "4", "8"})
        public int parallelism;

        Path input;
        ExecutorService executor;
        ParallelRangeProcessor<Storage> processor;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            executor = Executors.newFixedThreadPool(parallelism);
            processor = OneBrcStyleProcessorWorkload.newParallelRangeProcessor(
                    parallelism,
                    executor);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws InterruptedException {
            shutDown(executor);
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

    private static void shutDown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Benchmark executor did not terminate");
            }
        }
    }
}
