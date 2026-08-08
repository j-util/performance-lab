package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.parallelrangeprocessor.ParallelRangeProcessor;

class OneBrcStyleProcessorWorkloadTest {

    private static final int PARALLELISM = 4;
    private static final String BOUNDARY_STATION = "Boundary Station " + "x".repeat(200);
    private static final List<String> LINES = List.of(
            "Yerevan;-5.5",
            "Berlin;10.0",
            BOUNDARY_STATION + ";12.3",
            "Yerevan;7.5",
            "Berlin;-2.0",
            "Tokyo;0.0");

    private static final Map<String, ExpectedCounter> EXPECTED = expectedCounters();

    @TempDir
    Path temporaryDirectory;

    @Test
    void allImplementationsAgreeWhenTheFinalLineHasANewline() throws Exception {
        assertAllImplementations(writeFixture("with-newline.csv", true));
    }

    @Test
    void allImplementationsAgreeWhenTheFinalLineHasNoNewline() throws Exception {
        assertAllImplementations(writeFixture("without-newline.csv", false));
    }

    private Path writeFixture(String fileName, boolean trailingNewline) throws Exception {
        String content = String.join("\n", LINES) + (trailingNewline ? "\n" : "");
        assertBoundaryStationCrossesAProcessorRange(content);
        Path fixture = temporaryDirectory.resolve(fileName);
        Files.writeString(fixture, content, StandardCharsets.UTF_8);
        return fixture;
    }

    private static void assertAllImplementations(Path fixture) throws Exception {
        ForkJoinPool forkJoinPool = new ForkJoinPool(PARALLELISM);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(PARALLELISM);
        try {
            ParallelRangeProcessor<Storage> forkJoinProcessor =
                    OneBrcStyleProcessorWorkload.newParallelRangeProcessor(
                            PARALLELISM,
                            forkJoinPool,
                            7);
            ParallelRangeProcessor<Storage> fixedThreadProcessor =
                    OneBrcStyleProcessorWorkload.newParallelRangeProcessor(
                            PARALLELISM,
                            fixedThreadPool,
                            7);

            assertExpected(OneBrcStyleProcessorWorkload.filesLinesSequential(fixture));
            assertExpected(OneBrcStyleProcessorWorkload.filesLinesParallel(fixture, forkJoinPool));
            assertExpected(OneBrcStyleProcessorWorkload.inputStreamProcessorCore(
                    fixture,
                    OneBrcStyleProcessorWorkload.newInputStreamProcessor()));
            assertExpected(OneBrcStyleProcessorWorkload.parallelRangeProcessor(
                    fixture,
                    forkJoinProcessor));
            assertExpected(OneBrcStyleProcessorWorkload.parallelRangeProcessor(
                    fixture,
                    fixedThreadProcessor));
        } finally {
            shutDown(forkJoinPool);
            shutDown(fixedThreadPool);
        }
    }

    private static void assertExpected(Storage actual) {
        assertEquals(LINES.size(), actual.totalProcessedRowCount());
        assertEquals(EXPECTED.keySet(), actual.counters().keySet());
        EXPECTED.forEach((station, expected) -> {
            Counter actualCounter = actual.counters().get(station);
            assertEquals(expected.min(), actualCounter.min());
            assertEquals(expected.max(), actualCounter.max());
            assertEquals(expected.sum(), actualCounter.sum(), tolerance(expected.sum()));
            assertEquals(expected.count(), actualCounter.count());
            assertEquals(expected.mean(), actualCounter.mean(), tolerance(expected.mean()));
        });
    }

    private static void assertBoundaryStationCrossesAProcessorRange(String content) {
        byte[] input = content.getBytes(StandardCharsets.UTF_8);
        long recordStart = (LINES.get(0) + "\n" + LINES.get(1) + "\n")
                .getBytes(StandardCharsets.UTF_8).length;
        long recordEnd = recordStart + LINES.get(2).getBytes(StandardCharsets.UTF_8).length;
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

    private static double tolerance(double value) {
        return Math.max(1.0, Math.abs(value)) * 1.0e-12;
    }

    private static void shutDown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }

    private static Map<String, ExpectedCounter> expectedCounters() {
        Map<String, ExpectedCounter> expected = new LinkedHashMap<>();
        expected.put("Yerevan", new ExpectedCounter(-5.5, 7.5, 2.0, 2L, 1.0));
        expected.put("Berlin", new ExpectedCounter(-2.0, 10.0, 8.0, 2L, 4.0));
        expected.put(BOUNDARY_STATION, new ExpectedCounter(12.3, 12.3, 12.3, 1L, 12.3));
        expected.put("Tokyo", new ExpectedCounter(0.0, 0.0, 0.0, 1L, 0.0));
        return expected;
    }

    private record ExpectedCounter(
            double min,
            double max,
            double sum,
            long count,
            double mean
    ) {
    }
}
