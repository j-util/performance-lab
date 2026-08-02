package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
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

    @Test
    void allEndToEndReductionStrategiesProcessTheSameRowsAndProduceTheSameFilteredPriceSum()
            throws Exception {
        int rowCount = 3;
        Path dataset = temporaryDirectory.resolve("filtered-price-rows.csv");
        Files.writeString(dataset, """
                id,customerId,productId,quantity,priceCents,timestamp,region,status
                1,101,11,4,100,1704067200000,EUROPE,COMPLETED
                2,102,12,5,200,1704067201000,ASIA_PACIFIC,PENDING
                3,103,13,6,300,1704067202000,NORTH_AMERICA,REFUNDED
                """);

        CsvProcessingStrategies.FilteredPriceSumResult arrayList =
                CsvProcessingStrategies.arrayListFilteredPriceSumEndToEnd(dataset, rowCount);
        CsvProcessingStrategies.FilteredPriceSumResult columnar =
                CsvProcessingStrategies.columnarFilteredPriceSumEndToEnd(dataset, rowCount);
        CsvProcessingStrategies.FilteredPriceSumResult reductionStore =
                CsvProcessingStrategies.reductionStoreFilteredPriceSumEndToEnd(dataset, rowCount);

        assertEquals(rowCount, arrayList.rowCount());
        assertEquals(rowCount, columnar.rowCount());
        assertEquals(rowCount, reductionStore.rowCount());
        assertEquals(arrayList.sum(), columnar.sum());
        assertEquals(arrayList.sum(), reductionStore.sum());
        assertEquals(500L, arrayList.sum());
    }

    private static void assertSameResult(
            CsvProcessingStrategies.StrategyResult expected,
            CsvProcessingStrategies.StrategyResult actual) {
        assertEquals(expected.rowCount(), actual.rowCount());
        assertEquals(expected.checksum(), actual.checksum());
    }
}
