package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import io.github.jutil.inputstreamprocessor.core.InputParser;
import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.parallelrangeprocessor.FileRangeSource;
import io.github.jutil.parallelrangeprocessor.ParallelRangeProcessor;
import io.github.jutil.parallelrangeprocessor.RecordDelimiter;
import io.github.jutil.splicelist.SpliceList;

/** Shared file-to-collection operations for the parallel list-assembly benchmark. */
public final class SpliceListParallelCollectionWorkload {

    private SpliceListParallelCollectionWorkload() {
    }

    /**
     * Parses the file sequentially into one pre-sized {@link ArrayList}, preserving source order.
     */
    public static ArrayList<Item> sequentialArrayList(
            Path input,
            int rowCount,
            InputStreamProcessor<Item> processor
    ) throws IOException {
        ArrayList<Item> destination = new ArrayList<>(rowCount);
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, destination::add);
        }
        return destination;
    }

    /**
     * Collects parser-local array lists and copies them into one pre-sized destination.
     * Parallel partial-result order, and therefore global destination order, is unspecified.
     */
    public static ArrayList<Item> parallelArrayList(
            Path input,
            int rowCount,
            ParallelRangeProcessor<ArrayList<Item>> processor
    ) throws IOException, InterruptedException {
        ConcurrentLinkedQueue<ArrayList<Item>> partials = collectPartials(input, processor);
        return combineArrayLists(partials, rowCount);
    }

    /**
     * Collects parser-local splice lists and transfers their nodes into one destination.
     * Parallel partial-result order, and therefore global destination order, is unspecified.
     */
    public static SpliceList<Item> parallelSpliceList(
            Path input,
            ParallelRangeProcessor<SpliceList<Item>> processor
    ) throws IOException, InterruptedException {
        ConcurrentLinkedQueue<SpliceList<Item>> partials = collectPartials(input, processor);
        return combineSpliceLists(partials);
    }

    /** Creates the reusable sequential processor outside measured execution. */
    public static InputStreamProcessor<Item> newSequentialProcessor() {
        return new InputStreamProcessor<>(new ItemParser());
    }

    /** Creates the reusable range processor for parser-local array lists. */
    public static ParallelRangeProcessor<ArrayList<Item>> newArrayListProcessor(
            int parallelism,
            Executor executor
    ) {
        return newParallelProcessor(parallelism, executor, ArrayListParser::new);
    }

    /** Creates the reusable range processor for parser-local splice lists. */
    public static ParallelRangeProcessor<SpliceList<Item>> newSpliceListProcessor(
            int parallelism,
            Executor executor,
            int segmentSize
    ) {
        return newParallelProcessor(
                parallelism,
                executor,
                () -> new SpliceListParser(segmentSize));
    }

    static ParallelRangeProcessor<ArrayList<Item>> newArrayListProcessor(
            int parallelism,
            Executor executor,
            int readBufferSize
    ) {
        return newParallelProcessor(
                parallelism,
                executor,
                ArrayListParser::new,
                readBufferSize);
    }

    static ParallelRangeProcessor<SpliceList<Item>> newSpliceListProcessor(
            int parallelism,
            Executor executor,
            int segmentSize,
            int readBufferSize
    ) {
        return newParallelProcessor(
                parallelism,
                executor,
                () -> new SpliceListParser(segmentSize),
                readBufferSize);
    }

    static ArrayList<Item> combineArrayLists(
            Iterable<? extends ArrayList<Item>> partials,
            int rowCount
    ) {
        ArrayList<Item> destination = new ArrayList<>(rowCount);
        partials.forEach(destination::addAll);
        return destination;
    }

    static SpliceList<Item> combineSpliceLists(
            Iterable<? extends SpliceList<Item>> partials
    ) {
        SpliceList<Item> destination = new SpliceList<>();
        partials.forEach(destination::spliceTail);
        return destination;
    }

    private static <T> ConcurrentLinkedQueue<T> collectPartials(
            Path input,
            ParallelRangeProcessor<T> processor
    ) throws IOException, InterruptedException {
        ConcurrentLinkedQueue<T> partials = new ConcurrentLinkedQueue<>();
        processor.process(new FileRangeSource(input), partials::add);
        return partials;
    }

    private static <T> ParallelRangeProcessor<T> newParallelProcessor(
            int parallelism,
            Executor executor,
            java.util.function.Supplier<? extends InputParser<T>> parserFactory
    ) {
        return new ParallelRangeProcessor<>(
                parallelism,
                executor,
                parserFactory,
                RecordDelimiter.newline());
    }

    private static <T> ParallelRangeProcessor<T> newParallelProcessor(
            int parallelism,
            Executor executor,
            java.util.function.Supplier<? extends InputParser<T>> parserFactory,
            int readBufferSize
    ) {
        return new ParallelRangeProcessor<>(
                parallelism,
                executor,
                parserFactory,
                RecordDelimiter.newline(),
                readBufferSize);
    }

    private static final class ItemParser implements InputParser<Item> {

        @Override
        public void parse(InputStream input, Consumer<? super Item> emitter) throws IOException {
            OneBrcStyleCsvParser.parseItems(input, emitter);
        }
    }

    private static final class ArrayListParser implements InputParser<ArrayList<Item>> {

        @Override
        public void parse(
                InputStream input,
                Consumer<? super ArrayList<Item>> emitter
        ) throws IOException {
            ArrayList<Item> items = new ArrayList<>();
            OneBrcStyleCsvParser.parseItems(input, items::add);
            emitter.accept(items);
        }
    }

    private static final class SpliceListParser implements InputParser<SpliceList<Item>> {

        private final int segmentSize;

        private SpliceListParser(int segmentSize) {
            this.segmentSize = segmentSize;
        }

        @Override
        public void parse(
                InputStream input,
                Consumer<? super SpliceList<Item>> emitter
        ) throws IOException {
            SpliceList<Item> items = new SpliceList<>(segmentSize);
            OneBrcStyleCsvParser.parseItems(input, items::addLast);
            emitter.accept(items);
        }
    }
}
