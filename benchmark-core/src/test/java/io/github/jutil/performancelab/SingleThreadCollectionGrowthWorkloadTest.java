package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.splicelist.SpliceList;

class SingleThreadCollectionGrowthWorkloadTest {

    private static final List<Item> EXPECTED = expectedItems();

    @TempDir
    Path temporaryDirectory;

    @Test
    void bothCollectionsContainEveryItemInSourceOrderWhenFinalLineHasNewline()
            throws Exception {
        assertCompleteOrderedResults(writeFixture("with-newline.csv", EXPECTED, true));
    }

    @Test
    void bothCollectionsContainEveryItemInSourceOrderWhenFinalLineHasNoNewline()
            throws Exception {
        assertCompleteOrderedResults(writeFixture("without-newline.csv", EXPECTED, false));
    }

    @Test
    void repeatedInvocationsReturnIndependentCollectionsWithoutRetainedResults()
            throws Exception {
        Path full = writeFixture("full.csv", EXPECTED, true);
        Path empty = writeFixture("empty.csv", List.of(), false);
        List<Item> partialExpected = EXPECTED.subList(0, 7);
        Path partial = writeFixture("partial.csv", partialExpected, false);
        InputStreamProcessor<Item> processor =
                SpliceListParallelCollectionWorkload.newSequentialProcessor();

        ArrayList<Item> firstArray =
                SingleThreadCollectionGrowthWorkload.arrayListInitialCapacity10(full, processor);
        ArrayList<Item> secondArray =
                SingleThreadCollectionGrowthWorkload.arrayListInitialCapacity10(full, processor);
        ArrayList<Item> emptyArray =
                SingleThreadCollectionGrowthWorkload.arrayListInitialCapacity10(empty, processor);
        ArrayList<Item> partialArray =
                SingleThreadCollectionGrowthWorkload.arrayListInitialCapacity10(partial, processor);

        SpliceList<Item> firstSplice =
                SingleThreadCollectionGrowthWorkload.spliceListSegmentSize10(full, processor);
        SpliceList<Item> secondSplice =
                SingleThreadCollectionGrowthWorkload.spliceListSegmentSize10(full, processor);
        SpliceList<Item> emptySplice =
                SingleThreadCollectionGrowthWorkload.spliceListSegmentSize10(empty, processor);
        SpliceList<Item> partialSplice =
                SingleThreadCollectionGrowthWorkload.spliceListSegmentSize10(partial, processor);

        assertNotSame(firstArray, secondArray);
        assertNotSame(firstSplice, secondSplice);
        assertIterableEquals(EXPECTED, firstArray);
        assertIterableEquals(EXPECTED, secondArray);
        assertIterableEquals(EXPECTED, firstSplice);
        assertIterableEquals(EXPECTED, secondSplice);
        assertTrue(emptyArray.isEmpty());
        assertTrue(emptySplice.isEmpty());
        assertIterableEquals(partialExpected, partialArray);
        assertIterableEquals(partialExpected, partialSplice);
    }

    private void assertCompleteOrderedResults(Path fixture) throws Exception {
        assertTrue(EXPECTED.size() > 10);
        InputStreamProcessor<Item> processor =
                SpliceListParallelCollectionWorkload.newSequentialProcessor();

        ArrayList<Item> arrayList =
                SingleThreadCollectionGrowthWorkload.arrayListInitialCapacity10(
                        fixture,
                        processor);
        SpliceList<Item> spliceList =
                SingleThreadCollectionGrowthWorkload.spliceListSegmentSize10(
                        fixture,
                        processor);

        assertEquals(EXPECTED.size(), arrayList.size());
        assertEquals(EXPECTED.size(), spliceList.size());
        assertIterableEquals(EXPECTED, arrayList);
        assertIterableEquals(EXPECTED, spliceList);
        assertEquals(arrayList, spliceList);
    }

    private Path writeFixture(
            String fileName,
            List<Item> items,
            boolean trailingNewline
    ) throws Exception {
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                content.append('\n');
            }
            Item item = items.get(index);
            content.append(item.key()).append(';').append(item.value());
        }
        if (trailingNewline && !items.isEmpty()) {
            content.append('\n');
        }
        Path fixture = temporaryDirectory.resolve(fileName);
        Files.writeString(fixture, content, StandardCharsets.UTF_8);
        return fixture;
    }

    private static List<Item> expectedItems() {
        ArrayList<Item> items = new ArrayList<>(27);
        for (int index = 0; index < 27; index++) {
            items.add(new Item("Station " + index, index - 13.0));
        }
        return List.copyOf(items);
    }
}
