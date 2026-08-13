package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.jutil.splicelist.SpliceList;

class ReadyCollectionIterationCasesTest {

    @Test
    void allRepresentationsContainIdenticalItemsInEncounterOrderAcrossManySegments() {
        int rowCount = 10_000;
        ArrayList<Item> arrayList = ReadyCollectionIterationCases.newArrayList(rowCount);
        SpliceList<Item> oneSegment =
                ReadyCollectionIterationCases.newOneSegmentSpliceList(rowCount);
        SpliceList<Item> tenSegment =
                ReadyCollectionIterationCases.newTenSegmentSpliceList(rowCount);

        assertIterableEquals(arrayList, oneSegment);
        assertIterableEquals(arrayList, tenSegment);
    }

    @Test
    void allIteratorSumsAgreeExactly() {
        int rowCount = 1_000;
        double arrayListSum = ReadyCollectionIterationCases.iteratorSum(
                ReadyCollectionIterationCases.newArrayList(rowCount));
        double oneSegmentSum = ReadyCollectionIterationCases.iteratorSum(
                ReadyCollectionIterationCases.newOneSegmentSpliceList(rowCount));
        double tenSegmentSum = ReadyCollectionIterationCases.iteratorSum(
                ReadyCollectionIterationCases.newTenSegmentSpliceList(rowCount));

        assertEquals(arrayListSum, oneSegmentSum);
        assertEquals(arrayListSum, tenSegmentSum);
    }

    @Test
    void emptyIterableProducesZeroSum() {
        assertEquals(0.0, ReadyCollectionIterationCases.iteratorSum(List.of()));
    }

    @Test
    void divisibleSizeUsesOneTenthAsTheRegularSegmentCapacity() {
        int rowCount = 100;
        assertEquals(10, ReadyCollectionIterationCases.tenSegmentSize(rowCount));

        SpliceList<Item> items =
                ReadyCollectionIterationCases.newTenSegmentSpliceList(rowCount);

        assertIterableEquals(expectedItems(rowCount), items);
    }

    @Test
    void nonDivisibleSizeUsesCeilingDivisionAndRetainsEveryItem() {
        int rowCount = 23;
        assertEquals(3, ReadyCollectionIterationCases.tenSegmentSize(rowCount));

        SpliceList<Item> items =
                ReadyCollectionIterationCases.newTenSegmentSpliceList(rowCount);

        assertIterableEquals(expectedItems(rowCount), items);
    }

    @Test
    void repeatedTraversalsReturnTheSameResult() {
        SpliceList<Item> items =
                ReadyCollectionIterationCases.newTenSegmentSpliceList(257);

        double first = ReadyCollectionIterationCases.iteratorSum(items);
        double second = ReadyCollectionIterationCases.iteratorSum(items);

        assertEquals(first, second);
    }

    private static List<Item> expectedItems(int rowCount) {
        ArrayList<Item> expected = new ArrayList<>(rowCount);
        for (int index = 0; index < rowCount; index++) {
            expected.add(ReadyCollectionIterationCases.itemAt(index));
        }
        return expected;
    }
}
