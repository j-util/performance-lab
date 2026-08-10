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

    private static final int BATCH_SIZE = 4_096;

    @Param({"1000000", "10000000"})
    public int rowCount;

    private Path parquetFile;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        parquetFile = HardwoodParquetDatasetGenerator.writeTemporary(rowCount);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        if (parquetFile != null) {
            Files.deleteIfExists(parquetFile);
        }
    }

    @Benchmark
    public ProjectionStore<HardwoodMarketDataProjection> hardwoodToColumnarBatch()
            throws IOException {
        return HardwoodMaterializationCases.hardwoodToColumnarBatch(
                parquetFile, rowCount, BATCH_SIZE);
    }

    @Benchmark
    public ArrayList<HardwoodMarketDataRow> hardwoodToArrayList() throws IOException {
        return HardwoodMaterializationCases.hardwoodToArrayList(
                parquetFile, rowCount, BATCH_SIZE);
    }
}
