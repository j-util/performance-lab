package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.jutil.splicelist.SpliceList;

class ParallelListFillAndCombineWorkloadTest {

    @Test
    void unevenPartitionsRetainExactSizeAndEncounterOrder() throws Exception {
        Object[] markers = newMarkers(10);

        assertEquals(3, ParallelListFillAndCombineWorkload.activePartitionCount(10, 3));
        assertEquals(4, ParallelListFillAndCombineWorkload.expectedPartitionSize(10, 3, 0));
        assertEquals(3, ParallelListFillAndCombineWorkload.expectedPartitionSize(10, 3, 1));
        assertEquals(3, ParallelListFillAndCombineWorkload.expectedPartitionSize(10, 3, 2));
        assertEquals(0, ParallelListFillAndCombineWorkload.partitionStart(10, 3, 0));
        assertEquals(4, ParallelListFillAndCombineWorkload.partitionStart(10, 3, 1));
        assertEquals(7, ParallelListFillAndCombineWorkload.partitionStart(10, 3, 2));

        assertBothResults(markers, 3);
    }

    @Test
    void fewerElementsThanWorkersUseOnlyNonEmptyPartitions() throws Exception {
        Object[] markers = newMarkers(3);

        assertEquals(3, ParallelListFillAndCombineWorkload.activePartitionCount(3, 8));
        for (int partitionIndex = 0; partitionIndex < markers.length; partitionIndex++) {
            assertEquals(
                    1,
                    ParallelListFillAndCombineWorkload.expectedPartitionSize(
                            markers.length,
                            markers.length,
                            partitionIndex));
        }

        assertBothResults(markers, 8);
    }

    @Test
    void repeatedInvocationsReturnIndependentCompleteCollections() throws Exception {
        Object[] markers = newMarkers(17);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            ArrayList<Object> firstArray =
                    ParallelListFillAndCombineWorkload.arrayListAddAll(markers, 4, executor);
            ArrayList<Object> secondArray =
                    ParallelListFillAndCombineWorkload.arrayListAddAll(markers, 4, executor);
            SpliceList<Object> firstSplice =
                    ParallelListFillAndCombineWorkload.spliceListSpliceTail(markers, 4, executor);
            SpliceList<Object> secondSplice =
                    ParallelListFillAndCombineWorkload.spliceListSpliceTail(markers, 4, executor);

            assertNotSame(firstArray, secondArray);
            assertNotSame(firstSplice, secondSplice);
            assertExactReferences(markers, firstArray);
            assertExactReferences(markers, secondArray);
            assertExactReferences(markers, firstSplice);
            assertExactReferences(markers, secondSplice);
        } finally {
            shutDown(executor);
        }
    }

    @Test
    void consolidationEmptiesEverySpliceListSource() {
        Object[] markers = newMarkers(6);
        SpliceList<Object> first = spliceListOf(markers, 0, 2);
        SpliceList<Object> second = spliceListOf(markers, 2, 1);
        SpliceList<Object> third = spliceListOf(markers, 3, 3);
        List<SpliceList<Object>> sources = List.of(first, second, third);

        SpliceList<Object> destination =
                ParallelListFillAndCombineWorkload.combineSpliceLists(sources);

        assertExactReferences(markers, destination);
        for (SpliceList<Object> source : sources) {
            assertTrue(source.isEmpty());
        }
    }

    @Test
    void mergeOnlyInputsCanBeRecreatedAfterDestructiveConsolidation() {
        Object[] markers = newMarkers(11);
        List<SpliceList<Object>> firstSources =
                ParallelListFillAndCombineWorkload.prepareSpliceListPartials(markers, 4);
        SpliceList<Object> first =
                ParallelListFillAndCombineWorkload.combineSpliceLists(firstSources);
        List<SpliceList<Object>> secondSources =
                ParallelListFillAndCombineWorkload.prepareSpliceListPartials(markers, 4);
        SpliceList<Object> second =
                ParallelListFillAndCombineWorkload.combineSpliceLists(secondSources);

        assertExactReferences(markers, first);
        assertExactReferences(markers, second);
        firstSources.forEach(source -> assertTrue(source.isEmpty()));
        secondSources.forEach(source -> assertTrue(source.isEmpty()));
    }

    private static void assertBothResults(Object[] markers, int parallelism) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            ArrayList<Object> arrayList =
                    ParallelListFillAndCombineWorkload.arrayListAddAll(
                            markers,
                            parallelism,
                            executor);
            SpliceList<Object> spliceList =
                    ParallelListFillAndCombineWorkload.spliceListSpliceTail(
                            markers,
                            parallelism,
                            executor);

            assertExactReferences(markers, arrayList);
            assertExactReferences(markers, spliceList);
        } finally {
            shutDown(executor);
        }
    }

    private static void assertExactReferences(Object[] expected, List<Object> actual) {
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) {
            assertSame(expected[index], actual.get(index));
        }
    }

    private static Object[] newMarkers(int count) {
        Object[] markers = new Object[count];
        for (int index = 0; index < count; index++) {
            markers[index] = new Object();
        }
        return markers;
    }

    private static SpliceList<Object> spliceListOf(
            Object[] markers,
            int start,
            int size
    ) {
        SpliceList<Object> result = new SpliceList<>(size);
        int end = start + size;
        for (int index = start; index < end; index++) {
            result.add(markers[index]);
        }
        return result;
    }

    private static void shutDown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
}
