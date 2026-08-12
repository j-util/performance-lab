package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import io.github.jutil.inputstreamprocessor.core.InputStreamProcessor;
import io.github.jutil.splicelist.SpliceList;

/** Shared file-to-collection operations for the single-thread collection-growth benchmark. */
public final class SingleThreadCollectionGrowthWorkload {

    private SingleThreadCollectionGrowthWorkload() {
    }

    /**
     * Parses the file into a fresh {@link ArrayList} with initial capacity 10.
     * The list grows geometrically by replacing and copying its backing array.
     */
    public static ArrayList<Item> arrayListInitialCapacity10(
            Path input,
            InputStreamProcessor<Item> processor
    ) throws IOException {
        ArrayList<Item> destination = new ArrayList<>(10);
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, destination::add);
        }
        return destination;
    }

    /**
     * Parses the file into a fresh {@link SpliceList} with regular segment size 10.
     * The list appends another ten-element segment when the current segment is full.
     */
    public static SpliceList<Item> spliceListSegmentSize10(
            Path input,
            InputStreamProcessor<Item> processor
    ) throws IOException {
        SpliceList<Item> destination = new SpliceList<>(10);
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, destination::addLast);
        }
        return destination;
    }
}
