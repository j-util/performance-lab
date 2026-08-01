package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/** Bulk end-to-end baseline for full-row CSV processing. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class CsvFullRowBenchmark {

    @Param({"10000"})
    public int rowCount;

    private Path csvFile;

    @Setup
    public void setup() {
        csvFile = Path.of(
                "target", "benchmark-data", "benchmark-rows-" + rowCount + ".csv");
        if (!Files.isRegularFile(csvFile)) {
            throw new IllegalStateException(
                    "CSV benchmark dataset does not exist: " + csvFile.toAbsolutePath().normalize()
                            + ". Generate it first with CsvDatasetGenerator " + rowCount + ".");
        }
    }

    @Benchmark
    public long streamingToConsumer() throws IOException {
        return CsvProcessingStrategies.streamingToConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long listMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.listMaterializationThenConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long columnarMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.columnarMaterializationThenConsumer(csvFile, rowCount).checksum();
    }
}
