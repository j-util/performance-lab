package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.parallelrangeprocessor.ParallelRangeProcessor;
import io.github.jutil.splicelist.SpliceList;

class SpliceListParallelCollectionWorkloadTest {

    private static final int PARALLELISM = 4;
    private static final String BOUNDARY_STATION = "Boundary Station " + "x".repeat(200);
    private static final List<Item> EXPECTED = List.of(
            new Item("Yerevan", -5.5),
            new Item("Berlin", 10.0),
            new Item(BOUNDARY_STATION, 12.3),
            new Item("Yerevan", -5.5),
            new Item("Berlin", -2.0),
            new Item("Tokyo", 0.0));

    @TempDir
    Path temporaryDirectory;

    @Test
    void allStrategiesAgreeWhenTheFinalLineHasANewlineAcrossRepeatedInvocations()
            throws Exception {
        assertAllStrategiesRepeatedly(writeFixture("with-newline.csv", true));
    }

    @Test
    void allStrategiesAgreeWhenTheFinalLineHasNoNewlineAcrossRepeatedInvocations()
            throws Exception {
        assertAllStrategiesRepeatedly(writeFixture("without-newline.csv", false));
    }

    @Test
    void arrayListAssemblyIncludesEveryPartialResult() {
        ArrayList<Item> first = new ArrayList<>(EXPECTED.subList(0, 2));
        ArrayList<Item> second = new ArrayList<>();
        ArrayList<Item> third = new ArrayList<>(EXPECTED.subList(2, EXPECTED.size()));

        ArrayList<Item> destination =
                SpliceListParallelCollectionWorkload.combineArrayLists(
                        List.of(first, second, third),
                        EXPECTED.size());

        assertEquals(EXPECTED, destination);
        assertEquals(EXPECTED.subList(0, 2), first);
        assertTrue(second.isEmpty());
        assertEquals(EXPECTED.subList(2, EXPECTED.size()), third);
    }

    @Test
    void spliceListAssemblyTransfersEveryPartialResultAndEmptiesTheSources() {
        SpliceList<Item> first = spliceListOf(EXPECTED.subList(0, 2));
        SpliceList<Item> second = new SpliceList<>();
        SpliceList<Item> third = spliceListOf(EXPECTED.subList(2, EXPECTED.size()));

        SpliceList<Item> destination =
                SpliceListParallelCollectionWorkload.combineSpliceLists(
                        List.of(first, second, third));

        assertEquals(EXPECTED, destination);
        assertTrue(first.isEmpty());
        assertTrue(second.isEmpty());
        assertTrue(third.isEmpty());
    }

    @Test
    void spliceListParserProducesExpectedResultBeyondConfiguredSegmentSize() throws Exception {
        Path fixture = writeFixture("multiple-splice-list-segments.csv", true);
        int segmentSize = 2;
        assertTrue(EXPECTED.size() > segmentSize);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ParallelRangeProcessor<SpliceList<Item>> processor =
                    SpliceListParallelCollectionWorkload.newSpliceListProcessor(
                            1,
                            executor,
                            segmentSize,
                            7);

            SpliceList<Item> actual = runParallelSplice(fixture, processor);

            assertEquals(EXPECTED, actual);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private Path writeFixture(String fileName, boolean trailingNewline) throws Exception {
        String content = EXPECTED.stream()
                .map(item -> item.key() + ";" + item.value())
                .collect(java.util.stream.Collectors.joining("\n"))
                + (trailingNewline ? "\n" : "");
        assertBoundaryRecordCrossesAProcessorRange(content);
        Path fixture = temporaryDirectory.resolve(fileName);
        Files.writeString(fixture, content, StandardCharsets.UTF_8);
        return fixture;
    }

    private static void assertAllStrategiesRepeatedly(Path fixture) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);
        try {
            InputStreamProcessor<Item> sequentialProcessor =
                    SpliceListParallelCollectionWorkload.newSequentialProcessor();
            ParallelRangeProcessor<ArrayList<Item>> arrayListProcessor =
                    SpliceListParallelCollectionWorkload.newArrayListProcessor(
                            PARALLELISM,
                            executor,
                            7);
            ParallelRangeProcessor<SpliceList<Item>> spliceListProcessor =
                    SpliceListParallelCollectionWorkload.newSpliceListProcessor(
                            PARALLELISM,
                            executor,
                            EXPECTED.size(),
                            7);

            ArrayList<Item> firstSequential = runSequential(fixture, sequentialProcessor);
            ArrayList<Item> secondSequential = runSequential(fixture, sequentialProcessor);
            ArrayList<Item> firstParallelArray = runParallelArray(fixture, arrayListProcessor);
            ArrayList<Item> secondParallelArray = runParallelArray(fixture, arrayListProcessor);
            SpliceList<Item> firstParallelSplice = runParallelSplice(
                    fixture,
                    spliceListProcessor);
            SpliceList<Item> secondParallelSplice = runParallelSplice(
                    fixture,
                    spliceListProcessor);

            assertNotSame(firstSequential, secondSequential);
            assertNotSame(firstParallelArray, secondParallelArray);
            assertNotSame(firstParallelSplice, secondParallelSplice);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static ArrayList<Item> runSequential(
            Path fixture,
            InputStreamProcessor<Item> processor
    ) throws Exception {
        ArrayList<Item> actual = SpliceListParallelCollectionWorkload.sequentialArrayList(
                fixture,
                EXPECTED.size(),
                processor);
        assertEquals(EXPECTED, actual);
        assertExpectedMultiset(actual);
        return actual;
    }

    private static ArrayList<Item> runParallelArray(
            Path fixture,
            ParallelRangeProcessor<ArrayList<Item>> processor
    ) throws Exception {
        ArrayList<Item> actual = SpliceListParallelCollectionWorkload.parallelArrayList(
                fixture,
                EXPECTED.size(),
                processor);
        assertExpectedMultiset(actual);
        return actual;
    }

    private static SpliceList<Item> runParallelSplice(
            Path fixture,
            ParallelRangeProcessor<SpliceList<Item>> processor
    ) throws Exception {
        SpliceList<Item> actual = SpliceListParallelCollectionWorkload.parallelSpliceList(
                fixture,
                processor);
        assertExpectedMultiset(actual);
        return actual;
    }

    private static void assertExpectedMultiset(List<Item> actual) {
        assertEquals(EXPECTED.size(), actual.size());
        assertEquals(frequencies(EXPECTED), frequencies(actual));
    }

    private static Map<Item, Integer> frequencies(List<Item> items) {
        Map<Item, Integer> frequencies = new HashMap<>();
        items.forEach(item -> frequencies.merge(item, 1, Integer::sum));
        return frequencies;
    }

    private static SpliceList<Item> spliceListOf(List<Item> items) {
        SpliceList<Item> result = new SpliceList<>();
        items.forEach(result::addLast);
        return result;
    }

    private static void assertBoundaryRecordCrossesAProcessorRange(String content) {
        byte[] input = content.getBytes(StandardCharsets.UTF_8);
        long recordStart = (EXPECTED.get(0).key() + ";" + EXPECTED.get(0).value() + "\n"
                + EXPECTED.get(1).key() + ";" + EXPECTED.get(1).value() + "\n")
                .getBytes(StandardCharsets.UTF_8).length;
        Item boundaryItem = EXPECTED.get(2);
        long recordEnd = recordStart
                + (boundaryItem.key() + ";" + boundaryItem.value())
                        .getBytes(StandardCharsets.UTF_8).length;
        long baseLength = input.length / PARALLELISM;
        long longerRangeCount = input.length % PARALLELISM;
        long boundary = 0L;
        boolean crossesBoundary = false;
        for (int range = 0; range < PARALLELISM - 1; range++) {
            boundary += baseLength + (range < longerRangeCount ? 1L : 0L);
            crossesBoundary |= boundary > recordStart && boundary < recordEnd;
        }
        assertTrue(crossesBoundary, "fixture must cross a real processor byte-range boundary");
    }
}
