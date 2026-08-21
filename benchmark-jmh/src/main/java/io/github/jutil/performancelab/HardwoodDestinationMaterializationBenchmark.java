package io.github.jutil.performancelab;

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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/** Destination allocation and filling over retained Hardwood-compatible column arrays. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@Threads(1)
@State(Scope.Benchmark)
public class HardwoodDestinationMaterializationBenchmark {

    private static final int PROJECTION_COLUMN_COUNT = 8;

    @Param({"1000000", "10000000"})
    public int rowCount;

    @Param({"8192"})
    public int batchSize;

    private HardwoodDestinationMaterializationCases.SourceArrays sourceArrays;
    private ExecutorService fixedColumnCopyExecutor;
    private ExecutorService virtualThreadPerTaskExecutor;

    @Setup(Level.Trial)
    public void setup() {
        sourceArrays = HardwoodDestinationMaterializationCases.createSourceArrays(rowCount);
        fixedColumnCopyExecutor = Executors.newFixedThreadPool(PROJECTION_COLUMN_COUNT);
        virtualThreadPerTaskExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        fixedColumnCopyExecutor.shutdown();
        virtualThreadPerTaskExecutor.shutdown();
        boolean fixedExecutorTerminated = awaitTermination(fixedColumnCopyExecutor);
        boolean virtualThreadExecutorTerminated = awaitTermination(virtualThreadPerTaskExecutor);
        if (!fixedExecutorTerminated || !virtualThreadExecutorTerminated) {
            throw new IllegalStateException(
                    "Column-copy executors did not terminate: fixed-pool="
                            + fixedExecutorTerminated
                            + ", virtual-thread-per-task="
                            + virtualThreadExecutorTerminated);
        }
    }

    private static boolean awaitTermination(ExecutorService executor)
            throws InterruptedException {
        if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
            return true;
        }
        executor.shutdownNow();
        return executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarSequentialRangedBatches() {
        return HardwoodDestinationMaterializationCases.sequentialRangedBatches(
                sourceArrays, batchSize);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarFixedPoolPerBatchBarrierAppender() {
        return HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                sourceArrays, batchSize, fixedColumnCopyExecutor);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarFixedPoolPipelinedAppender() {
        return HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                sourceArrays, batchSize, fixedColumnCopyExecutor);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore
            columnarVirtualThreadPerTaskPerBatchBarrierAppender() {
        return HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                sourceArrays, batchSize, virtualThreadPerTaskExecutor);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarVirtualThreadPerTaskPipelinedAppender() {
        return HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                sourceArrays, batchSize, virtualThreadPerTaskExecutor);
    }

    @Benchmark
    public ArrayList<HardwoodMarketDataRow> arrayListRows() {
        return HardwoodDestinationMaterializationCases.arrayListRows(
                sourceArrays, batchSize);
    }
}
