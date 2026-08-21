package io.github.jutil.performancelab;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Destination-only materialization paths over retained decoded column arrays. */
public final class HardwoodDestinationMaterializationCases {

    private HardwoodDestinationMaterializationCases() {}

    /** Creates the eight deterministic source arrays shared by all destination paths. */
    public static SourceArrays createSourceArrays(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("rowCount must be greater than zero");
        }

        long[] timestamps = new long[rowCount];
        String[] symbols = new String[rowCount];
        String[] venues = new String[rowCount];
        String[] sides = new String[rowCount];
        long[] sequenceNumbers = new long[rowCount];
        double[] bidPrices = new double[rowCount];
        double[] askPrices = new double[rowCount];
        double[] lastTradePrices = new double[rowCount];
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            HardwoodMarketDataRow row = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            timestamps[rowIndex] = row.timestamp();
            symbols[rowIndex] = row.symbol();
            venues[rowIndex] = row.venue();
            sides[rowIndex] = row.side();
            sequenceNumbers[rowIndex] = row.sequenceNumber();
            bidPrices[rowIndex] = row.bidPrice();
            askPrices[rowIndex] = row.askPrice();
            lastTradePrices[rowIndex] = row.lastTradePrice();
        }
        return new SourceArrays(
                timestamps,
                symbols,
                venues,
                sides,
                sequenceNumbers,
                bidPrices,
                askPrices,
                lastTradePrices);
    }

    /** Creates an exact-capacity store and fills it through sequential ranged batches. */
    public static HardwoodMarketDataProjectionStore sequentialRangedBatches(
            SourceArrays source,
            int batchSize) {
        Objects.requireNonNull(source, "source");
        requirePositiveBatchSize(batchSize);

        HardwoodMarketDataProjectionStore store =
                HardwoodMarketDataProjectionStore.create(source.rowCount());
        for (int fromIndex = 0; fromIndex < source.rowCount(); ) {
            int toIndex = batchEnd(fromIndex, source.rowCount(), batchSize);
            store.batch(fromIndex, toIndex)
                    .timestamp(source.timestamps())
                    .symbol(source.symbols())
                    .venue(source.venues())
                    .side(source.sides())
                    .sequenceNumber(source.sequenceNumbers())
                    .bidPrice(source.bidPrices())
                    .askPrice(source.askPrices())
                    .lastTradePrice(source.lastTradePrices())
                    .append();
            fromIndex = toIndex;
        }
        store.seal();
        return store;
    }

    /**
     * Creates an exact-capacity store and waits at a barrier after each executor-backed batch.
     */
    public static HardwoodMarketDataProjectionStore executorPerBatchBarrierColumnAppender(
            SourceArrays source,
            int batchSize,
            Executor columnCopyExecutor) {
        Objects.requireNonNull(source, "source");
        requirePositiveBatchSize(batchSize);
        Objects.requireNonNull(columnCopyExecutor, "columnCopyExecutor");

        HardwoodMarketDataProjectionStore store =
                HardwoodMarketDataProjectionStore.create(source.rowCount());
        HardwoodMarketDataProjectionStore.ColumnAppender appender = store.columnAppender();
        for (int fromIndex = 0; fromIndex < source.rowCount(); ) {
            int toIndex = batchEnd(fromIndex, source.rowCount(), batchSize);
            int batchFromIndex = fromIndex;
            int batchToIndex = toIndex;
            CompletableFuture.allOf(
                    CompletableFuture.runAsync(
                            () -> appender.timestamp(
                                    source.timestamps(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.symbol(
                                    source.symbols(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.venue(
                                    source.venues(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.side(
                                    source.sides(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.sequenceNumber(
                                    source.sequenceNumbers(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.bidPrice(
                                    source.bidPrices(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.askPrice(
                                    source.askPrices(), batchFromIndex, batchToIndex),
                            columnCopyExecutor),
                    CompletableFuture.runAsync(
                            () -> appender.lastTradePrice(
                                    source.lastTradePrices(), batchFromIndex, batchToIndex),
                            columnCopyExecutor))
                    .join();
            fromIndex = toIndex;
        }
        store.seal();
        return store;
    }

    /**
     * Creates an exact-capacity store using one ordered executor-backed pipeline per column.
     */
    public static HardwoodMarketDataProjectionStore executorPipelinedColumnAppender(
            SourceArrays source,
            int batchSize,
            Executor columnCopyExecutor) {
        Objects.requireNonNull(source, "source");
        requirePositiveBatchSize(batchSize);
        Objects.requireNonNull(columnCopyExecutor, "columnCopyExecutor");

        HardwoodMarketDataProjectionStore store =
                HardwoodMarketDataProjectionStore.create(source.rowCount());
        HardwoodMarketDataProjectionStore.ColumnAppender appender = store.columnAppender();
        CompletableFuture<Void> timestampTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> symbolTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> venueTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> sideTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> sequenceNumberTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> bidPriceTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> askPriceTail = CompletableFuture.completedFuture(null);
        CompletableFuture<Void> lastTradePriceTail = CompletableFuture.completedFuture(null);
        for (int fromIndex = 0; fromIndex < source.rowCount(); ) {
            int toIndex = batchEnd(fromIndex, source.rowCount(), batchSize);
            int batchFromIndex = fromIndex;
            int batchToIndex = toIndex;
            timestampTail = timestampTail.thenRunAsync(
                    () -> appender.timestamp(
                            source.timestamps(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            symbolTail = symbolTail.thenRunAsync(
                    () -> appender.symbol(source.symbols(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            venueTail = venueTail.thenRunAsync(
                    () -> appender.venue(source.venues(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            sideTail = sideTail.thenRunAsync(
                    () -> appender.side(source.sides(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            sequenceNumberTail = sequenceNumberTail.thenRunAsync(
                    () -> appender.sequenceNumber(
                            source.sequenceNumbers(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            bidPriceTail = bidPriceTail.thenRunAsync(
                    () -> appender.bidPrice(
                            source.bidPrices(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            askPriceTail = askPriceTail.thenRunAsync(
                    () -> appender.askPrice(
                            source.askPrices(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            lastTradePriceTail = lastTradePriceTail.thenRunAsync(
                    () -> appender.lastTradePrice(
                            source.lastTradePrices(), batchFromIndex, batchToIndex),
                    columnCopyExecutor);
            fromIndex = toIndex;
        }
        CompletableFuture.allOf(
                        timestampTail,
                        symbolTail,
                        venueTail,
                        sideTail,
                        sequenceNumberTail,
                        bidPriceTail,
                        askPriceTail,
                        lastTradePriceTail)
                .join();
        store.seal();
        return store;
    }

    /** Creates an exact-capacity ArrayList and fills it with immutable row objects. */
    public static ArrayList<HardwoodMarketDataRow> arrayListRows(
            SourceArrays source,
            int batchSize) {
        Objects.requireNonNull(source, "source");
        requirePositiveBatchSize(batchSize);

        ArrayList<HardwoodMarketDataRow> rows = new ArrayList<>(source.rowCount());
        for (int fromIndex = 0; fromIndex < source.rowCount(); ) {
            int toIndex = batchEnd(fromIndex, source.rowCount(), batchSize);
            for (int rowIndex = fromIndex; rowIndex < toIndex; rowIndex++) {
                rows.add(new HardwoodMarketDataRow(
                        source.timestamps()[rowIndex],
                        source.symbols()[rowIndex],
                        source.venues()[rowIndex],
                        source.sides()[rowIndex],
                        source.sequenceNumbers()[rowIndex],
                        source.bidPrices()[rowIndex],
                        source.askPrices()[rowIndex],
                        source.lastTradePrices()[rowIndex]));
            }
            fromIndex = toIndex;
        }
        return rows;
    }

    private static int batchEnd(int fromIndex, int rowCount, int batchSize) {
        return (int) Math.min((long) fromIndex + batchSize, rowCount);
    }

    private static void requirePositiveBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
    }

    /** The retained decoded columns prepared once and read by every destination path. */
    public record SourceArrays(
            long[] timestamps,
            String[] symbols,
            String[] venues,
            String[] sides,
            long[] sequenceNumbers,
            double[] bidPrices,
            double[] askPrices,
            double[] lastTradePrices) {

        /** Validates that all eight source columns are non-null and aligned. */
        public SourceArrays {
            Objects.requireNonNull(timestamps, "timestamps");
            Objects.requireNonNull(symbols, "symbols");
            Objects.requireNonNull(venues, "venues");
            Objects.requireNonNull(sides, "sides");
            Objects.requireNonNull(sequenceNumbers, "sequenceNumbers");
            Objects.requireNonNull(bidPrices, "bidPrices");
            Objects.requireNonNull(askPrices, "askPrices");
            Objects.requireNonNull(lastTradePrices, "lastTradePrices");
            int rowCount = timestamps.length;
            if (rowCount == 0) {
                throw new IllegalArgumentException("source arrays must not be empty");
            }
            if (symbols.length != rowCount
                    || venues.length != rowCount
                    || sides.length != rowCount
                    || sequenceNumbers.length != rowCount
                    || bidPrices.length != rowCount
                    || askPrices.length != rowCount
                    || lastTradePrices.length != rowCount) {
                throw new IllegalArgumentException("source arrays must have identical lengths");
            }
        }

        /** Returns the common number of source rows. */
        public int rowCount() {
            return timestamps.length;
        }
    }
}
