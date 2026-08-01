package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvProcessingStrategiesTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void allStrategiesProcessTheSameRowsAndProduceTheSameChecksum() throws Exception {
        int rowCount = 17;
        Path dataset = temporaryDirectory.resolve("rows.csv");
        CsvDatasetGenerator.generate(rowCount, dataset);

        CsvProcessingStrategies.StrategyResult streaming =
                CsvProcessingStrategies.streamingToConsumer(dataset, rowCount);
        CsvProcessingStrategies.StrategyResult list =
                CsvProcessingStrategies.listMaterializationThenConsumer(dataset, rowCount);
        CsvProcessingStrategies.StrategyResult columnar =
                CsvProcessingStrategies.columnarMaterializationThenConsumer(dataset, rowCount);

        assertEquals(rowCount, streaming.rowCount());
        assertEquals(rowCount, list.rowCount());
        assertEquals(rowCount, columnar.rowCount());
        assertEquals(streaming.checksum(), list.checksum());
        assertEquals(list.checksum(), columnar.checksum(),
                "columnar cursor traversal must match List.forEach traversal");
    }
}
