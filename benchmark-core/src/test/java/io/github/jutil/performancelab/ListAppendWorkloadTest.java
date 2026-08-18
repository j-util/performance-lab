package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListAppendWorkloadTest {

    private static final int[] CAPACITY_HINTS = {
        256, 1_024, 4_096, 10_000, 20_000, 30_000
    };

    @Test
    void everyBenchmarkPathHasTheRequestedSizeAndMarkerContent() {
        Object marker = new Object();

        for (List<Object> result : results(257, marker)) {
            assertEquals(257, result.size());
            for (Object element : result) {
                assertSame(marker, element);
            }
        }
    }

    @Test
    void matchedOrdinaryAddPathsProduceEquivalentListsForEveryCapacityHint() {
        Object marker = new Object();

        for (int capacityHint : CAPACITY_HINTS) {
            assertMatchedOrdinaryAddPaths(capacityHint, capacityHint, marker);
            assertMatchedOrdinaryAddPaths(capacityHint + 1, capacityHint, marker);
        }
    }

    @Test
    void emptyInputProducesFreshEmptyCollections() {
        Object marker = new Object();

        for (List<Object> result : results(0, marker)) {
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void repeatedRunsReturnIndependentCollectionsWithCompleteContent() {
        Object marker = new Object();
        List<List<Object>> first = results(33, marker);
        List<List<Object>> second = results(33, marker);

        assertEquals(first.size(), second.size());
        for (int index = 0; index < first.size(); index++) {
            assertNotSame(first.get(index), second.get(index));
            assertEquals(33, first.get(index).size());
            assertEquals(33, second.get(index).size());
            assertEquals(first.get(index), second.get(index));
        }
    }

    @Test
    void unusedCapacityMatchesCompletedSpliceListSegments() {
        assertEquals(0L, ListAppendWorkload.unusedSpliceListCapacity(0, 1_024));
        assertEquals(1_023L, ListAppendWorkload.unusedSpliceListCapacity(1, 1_024));
        assertEquals(0L, ListAppendWorkload.unusedSpliceListCapacity(1_024, 1_024));
        assertEquals(240L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 256));
        assertEquals(240L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 1_024));
        assertEquals(2_288L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 4_096));
        assertEquals(0L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 10_000));
        assertEquals(10_000L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 20_000));
        assertEquals(20_000L, ListAppendWorkload.unusedSpliceListCapacity(10_000, 30_000));
    }

    private static List<List<Object>> results(int elementCount, Object marker) {
        ArrayList<List<Object>> results = new ArrayList<>();
        results.add(ListAppendWorkload.arrayListDefaultGrowingAdd(elementCount, marker));
        results.add(ListAppendWorkload.arrayListExactFinalCapacityAdd(elementCount, marker));
        for (int capacityHint : CAPACITY_HINTS) {
            results.add(ListAppendWorkload.arrayListCapacityHintAdd(
                    elementCount,
                    capacityHint,
                    marker));
            results.add(ListAppendWorkload.spliceListSegmentSizeAdd(
                    elementCount,
                    capacityHint,
                    marker));
            results.add(ListAppendWorkload.spliceListSegmentSizeAddLast(
                    elementCount,
                    capacityHint,
                    marker));
        }
        return results;
    }

    private static void assertMatchedOrdinaryAddPaths(
            int elementCount,
            int capacityHint,
            Object marker
    ) {
        List<Object> arrayList = ListAppendWorkload.arrayListCapacityHintAdd(
                elementCount,
                capacityHint,
                marker);
        List<Object> spliceList = ListAppendWorkload.spliceListSegmentSizeAdd(
                elementCount,
                capacityHint,
                marker);
        String context = "elementCount=" + elementCount + ", capacityHint=" + capacityHint;

        assertEquals(elementCount, arrayList.size(), context);
        assertEquals(elementCount, spliceList.size(), context);
        assertEquals(arrayList, spliceList, context);

        Iterator<Object> arrayIterator = arrayList.iterator();
        Iterator<Object> spliceIterator = spliceList.iterator();
        while (arrayIterator.hasNext()) {
            Object arrayElement = arrayIterator.next();
            Object spliceElement = spliceIterator.next();
            assertSame(marker, arrayElement, context);
            assertSame(arrayElement, spliceElement, context);
        }
    }
}
