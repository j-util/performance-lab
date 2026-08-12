package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.splicelist.SpliceList;

/** End-to-end single-thread ingestion with two configurable starting-storage policies. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@Threads(1)
public class SingleThreadCollectionGrowthBenchmark {

    @Benchmark
    public ArrayList<Item> arrayListInitialCapacity(BenchmarkState state) throws IOException {
        return SingleThreadCollectionGrowthWorkload.arrayList(
                state.input,
                state.processor,
                state.storageSize);
    }

    @Benchmark
    public SpliceList<Item> spliceListSegmentSize(BenchmarkState state) throws IOException {
        return SingleThreadCollectionGrowthWorkload.spliceList(
                state.input,
                state.processor,
                state.storageSize);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"10000000", "20000000"})
        public int rowCount;

        @Param({"100"})
        public int storageSize;

        Path input;
        InputStreamProcessor<Item> processor;

        @Setup(Level.Trial)
        public void setup() {
            input = requireDataset(rowCount);
            processor = SpliceListParallelCollectionWorkload.newSequentialProcessor();
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
