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
     * Parses the file into a fresh {@link ArrayList} with the requested initial capacity.
     * The list grows geometrically by replacing and copying its backing array.
     */
    public static ArrayList<Item> arrayList(
            Path input,
            InputStreamProcessor<Item> processor,
            int initialCapacity
    ) throws IOException {
        ArrayList<Item> destination = new ArrayList<>(initialCapacity);
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, destination::add);
        }
        return destination;
    }

    /**
     * Parses the file into a fresh {@link SpliceList} with the requested regular segment size.
     * The list appends another same-sized segment when the current segment is full.
     */
    public static SpliceList<Item> spliceList(
            Path input,
            InputStreamProcessor<Item> processor,
            int segmentSize
    ) throws IOException {
        SpliceList<Item> destination = new SpliceList<>(segmentSize);
        try (InputStream stream = Files.newInputStream(input)) {
            processor.process(stream, destination::addLast);
        }
        return destination;
    }
}
