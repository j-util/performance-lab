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
    private ExecutorService columnCopyExecutor;

    @Setup(Level.Trial)
    public void setup() {
        sourceArrays = HardwoodDestinationMaterializationCases.createSourceArrays(rowCount);
        columnCopyExecutor = Executors.newFixedThreadPool(PROJECTION_COLUMN_COUNT);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws InterruptedException {
        columnCopyExecutor.shutdown();
        if (!columnCopyExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
            columnCopyExecutor.shutdownNow();
            if (!columnCopyExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Column-copy executor did not terminate");
            }
        }
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarSequentialRangedBatches() {
        return HardwoodDestinationMaterializationCases.sequentialRangedBatches(
                sourceArrays, batchSize);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarExecutorPerBatchBarrierAppender() {
        return HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(
                sourceArrays, batchSize, columnCopyExecutor);
    }

    @Benchmark
    public HardwoodMarketDataProjectionStore columnarExecutorPipelinedAppender() {
        return HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(
                sourceArrays, batchSize, columnCopyExecutor);
    }

    @Benchmark
    public ArrayList<HardwoodMarketDataRow> arrayListRows() {
        return HardwoodDestinationMaterializationCases.arrayListRows(
                sourceArrays, batchSize);
    }
}
