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

/** CSV benchmarks comparing end-to-end filtered price-sum strategies. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class CsvFilteredPriceSumEndToEndBenchmark {

    @Param({"10000"})
    public int rowCount;

    private Path csvFile;

    @Setup
    public void setup() {
        csvFile = CsvBenchmarkStateSupport.benchmarkCsvFile(rowCount);
    }

    @Benchmark
    public long arrayListFilteredPriceSumEndToEnd() throws IOException {
        return CsvProcessingStrategies.arrayListFilteredPriceSumEndToEnd(csvFile, rowCount).sum();
    }

    @Benchmark
    public long columnarFilteredPriceSumEndToEnd() throws IOException {
        return CsvProcessingStrategies.columnarFilteredPriceSumEndToEnd(csvFile, rowCount).sum();
    }

    @Benchmark
    public long reductionStoreFilteredPriceSumEndToEnd() throws IOException {
        return CsvProcessingStrategies.reductionStoreFilteredPriceSumEndToEnd(csvFile, rowCount).sum();
    }
}
