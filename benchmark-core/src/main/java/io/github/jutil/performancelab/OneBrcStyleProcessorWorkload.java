package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.parallelrangeprocessor.FileRangeSource;
import io.github.jutil.parallelrangeprocessor.ParallelRangeProcessor;
import io.github.jutil.parallelrangeprocessor.RecordDelimiter;

/** The five comparable processing paths used by the 1BRC-style JMH suite and tests. */
public final class OneBrcStyleProcessorWorkload {

    private OneBrcStyleProcessorWorkload() {
    }

    /** Processes the file with a sequential {@link Files#lines(Path)} stream. */
    public static Storage filesLinesSequential(Path input) throws IOException {
        try (Stream<String> lines = Files.lines(input, StandardCharsets.UTF_8)) {
            return lines.collect(
                    Storage::new,
                    OneBrcStyleProcessorWorkload::storeParsedLine,
                    Storage::merge);
        }
    }

    /** Processes a parallel {@link Files#lines(Path)} stream inside the supplied pool. */
    public static Storage filesLinesParallel(Path input, ForkJoinPool pool)
            throws InterruptedException, ExecutionException {
        return pool.submit(() -> {
            try (Stream<String> lines = Files.lines(input, StandardCharsets.UTF_8)) {
                return lines.parallel().collect(
                        Storage::new,
                        OneBrcStyleProcessorWorkload::storeParsedLine,
                        Storage::merge);
            }
        }).get();
    }

    /** Processes the complete stream synchronously with inputstream-processor-core. */
    public static Storage inputStreamProcessorCore(
            Path input,
            InputStreamProcessor<Storage> processor
    ) throws IOException {
        Storage result = new Storage();
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, result::merge);
        }
        return result;
    }

    /** Processes file ranges and merges parser-owned partial aggregations afterward. */
    public static Storage parallelRangeProcessor(
            Path input,
            ParallelRangeProcessor<Storage> processor
    ) throws IOException, InterruptedException {
        ConcurrentLinkedQueue<Storage> partials = new ConcurrentLinkedQueue<>();
        processor.process(new FileRangeSource(input), partials::add);

        Storage result = new Storage();
        partials.forEach(result::merge);
        return result;
    }

    /** Creates the reusable sequential processor outside measured execution. */
    public static InputStreamProcessor<Storage> newInputStreamProcessor() {
        return new InputStreamProcessor<>(new OneBrcStyleCsvParser());
    }

    /** Creates a reusable range processor using its standard 64 KiB read buffer. */
    public static ParallelRangeProcessor<Storage> newParallelRangeProcessor(
            int parallelism,
            Executor executor
    ) {
        return new ParallelRangeProcessor<>(
                parallelism,
                executor,
                OneBrcStyleCsvParser::new,
                RecordDelimiter.newline());
    }

    /** Creates a range processor with a test-controlled real framing-buffer size. */
    static ParallelRangeProcessor<Storage> newParallelRangeProcessor(
            int parallelism,
            Executor executor,
            int readBufferSize
    ) {
        return new ParallelRangeProcessor<>(
                parallelism,
                executor,
                OneBrcStyleCsvParser::new,
                RecordDelimiter.newline(),
                readBufferSize);
    }

    private static void storeParsedLine(Storage storage, String line) {
        storage.store(OneBrcStyleCsvParser.parseLine(line));
    }
}
