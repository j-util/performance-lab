package io.github.jutil.performancelab;

import java.io.IOException;
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

/** CSV benchmarks comparing full-row processing and materialization strategies. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class CsvFullRowProcessingBenchmark {

    @Param({"10000"})
    public int rowCount;

    private Path csvFile;

    @Setup
    public void setup() {
        csvFile = CsvBenchmarkStateSupport.benchmarkCsvFile(rowCount);
    }

    @Benchmark
    public long streamingToConsumer() throws IOException {
        return CsvProcessingStrategies.streamingToConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long arrayListExpectedSizeMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .arrayListExpectedSizeMaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long arrayListInitial10MaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .arrayListInitial10MaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long linkedListMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies.linkedListMaterializationThenConsumer(csvFile, rowCount).checksum();
    }

    @Benchmark
    public long columnarExpectedSizeMaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .columnarExpectedSizeMaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }

    @Benchmark
    public long columnarInitial10MaterializationThenConsumer() throws IOException {
        return CsvProcessingStrategies
                .columnarInitial10MaterializationThenConsumer(csvFile, rowCount)
                .checksum();
    }
}
