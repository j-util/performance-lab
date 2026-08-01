package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvProcessingStrategiesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void allMaterializationStrategiesProcessTheSameRowsAndProduceTheSameChecksum() throws Exception {
        int rowCount = 17;
        Path dataset = temporaryDirectory.resolve("rows.csv");
        CsvDatasetGenerator.generate(rowCount, dataset);

        CsvProcessingStrategies.StrategyResult streaming =
                CsvProcessingStrategies.streamingToConsumer(dataset, rowCount);
        CsvProcessingStrategies.StrategyResult arrayListExpectedSize =
                CsvProcessingStrategies.arrayListExpectedSizeMaterializationThenConsumer(
                        dataset, rowCount);
        CsvProcessingStrategies.StrategyResult arrayListInitial10 =
                CsvProcessingStrategies.arrayListInitial10MaterializationThenConsumer(
                        dataset, rowCount);
        CsvProcessingStrategies.StrategyResult linkedList =
                CsvProcessingStrategies.linkedListMaterializationThenConsumer(dataset, rowCount);
        CsvProcessingStrategies.StrategyResult columnarExpectedSize =
                CsvProcessingStrategies.columnarExpectedSizeMaterializationThenConsumer(
                        dataset, rowCount);
        CsvProcessingStrategies.StrategyResult columnarInitial10 =
                CsvProcessingStrategies.columnarInitial10MaterializationThenConsumer(
                        dataset, rowCount);

        assertEquals(rowCount, streaming.rowCount());
        assertSameResult(streaming, arrayListExpectedSize);
        assertSameResult(arrayListExpectedSize, arrayListInitial10);
        assertSameResult(arrayListExpectedSize, linkedList);
        assertSameResult(arrayListExpectedSize, columnarExpectedSize);
        assertSameResult(columnarExpectedSize, columnarInitial10);
    }

    private static void assertSameResult(
            CsvProcessingStrategies.StrategyResult expected,
            CsvProcessingStrategies.StrategyResult actual) {
        assertEquals(expected.rowCount(), actual.rowCount());
        assertEquals(expected.checksum(), actual.checksum());
    }
}
