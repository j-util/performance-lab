package io.github.jutil.performancelab;

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

import io.github.jutil.performancelab.CsvBenchmarkStateSupport.ArrayListScanState;
import io.github.jutil.performancelab.CsvBenchmarkStateSupport.ColumnarScanState;
import io.github.jutil.performancelab.CsvBenchmarkStateSupport.LinkedListScanState;

/** Price-sum benchmarks over already-materialized, representation-specific data. */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(1)
@State(Scope.Benchmark)
public class ReadyPriceSumBenchmark {

    @Param({"10000"})
    public int rowCount;

    private Path csvFile;

    @Setup
    public void setup() {
        csvFile = CsvBenchmarkStateSupport.benchmarkCsvFile(rowCount);
    }

    @Benchmark
    public long arrayListPriceSum(ArrayListScanState state) {
        return CsvPriceSumScans.arrayListPriceSum(state.rows);
    }

    @Benchmark
    public long linkedListPriceSum(LinkedListScanState state) {
        return CsvPriceSumScans.linkedListPriceSum(state.rows);
    }

    @Benchmark
    public long columnarPriceSum(ColumnarScanState state) {
        return CsvPriceSumScans.columnarPriceSum(state.store);
    }
}
