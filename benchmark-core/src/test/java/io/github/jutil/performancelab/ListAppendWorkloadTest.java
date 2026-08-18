package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListAppendWorkloadTest {

    private static final int[] SEGMENT_SIZES = {256, 1_024, 4_096, 10_000};

    @Test
    void everyRepresentationHasTheRequestedSizeAndMarkerContent() {
        Object marker = new Object();

        for (List<Object> result : results(257, marker)) {
            assertEquals(257, result.size());
            for (Object element : result) {
                assertSame(marker, element);
            }
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
    }

    private static List<List<Object>> results(int elementCount, Object marker) {
        ArrayList<List<Object>> results = new ArrayList<>();
        results.add(ListAppendWorkload.arrayListAdd(elementCount, marker));
        results.add(ListAppendWorkload.arrayListExactCapacityAdd(elementCount, marker));
        for (int segmentSize : SEGMENT_SIZES) {
            results.add(ListAppendWorkload.spliceListAdd(
                    elementCount,
                    segmentSize,
                    marker));
            results.add(ListAppendWorkload.spliceListAddLast(
                    elementCount,
                    segmentSize,
                    marker));
        }
        return results;
    }
}
