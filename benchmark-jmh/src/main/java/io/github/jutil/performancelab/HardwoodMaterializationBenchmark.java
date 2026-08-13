package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

import io.github.jutil.columnarprojection.ProjectionStore;

/** End-to-end Hardwood decoding and destination materialization benchmarks. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class HardwoodMaterializationBenchmark {

    private static final int PROJECTION_COLUMN_COUNT = 8;

    @Param({"1000000", "10000000"})
    public int rowCount;

    private List<Path> parquetFiles;
    private ExecutorService columnCopyExecutor;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        parquetFiles = HardwoodParquetDatasetGenerator.writeTemporary(rowCount);
        columnCopyExecutor = Executors.newFixedThreadPool(PROJECTION_COLUMN_COUNT);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        try {
            shutDown(columnCopyExecutor);
        } finally {
            if (parquetFiles != null) {
                Files.deleteIfExists(parquetFiles.get(0));
                Files.deleteIfExists(parquetFiles.get(1));
            }
        }
    }

    @Benchmark
    public ProjectionStore<HardwoodMarketDataProjection> hardwoodToColumnarBatch()
            throws IOException {
        return HardwoodMaterializationCases.hardwoodToColumnarBatch(parquetFiles);
    }

    @Benchmark
    public ProjectionStore<HardwoodMarketDataProjection>
            hardwoodToExecutorBackedColumnarBatch() throws IOException {
        return HardwoodMaterializationCases.hardwoodToExecutorBackedColumnarBatch(
                parquetFiles, columnCopyExecutor);
    }

    @Benchmark
    public ArrayList<HardwoodMarketDataRow> hardwoodToArrayList() throws IOException {
        return HardwoodMaterializationCases.hardwoodToArrayList(parquetFiles);
    }

    private static void shutDown(ExecutorService executor)
            throws InterruptedException {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Column-copy executor did not terminate");
            }
        }
    }
}
