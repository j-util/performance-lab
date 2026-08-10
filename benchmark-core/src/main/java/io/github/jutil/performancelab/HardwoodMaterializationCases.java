package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import dev.hardwood.InputFile;
import dev.hardwood.reader.ColumnReader;
import dev.hardwood.reader.ColumnReaders;
import dev.hardwood.reader.ParquetFileReader;
import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;

/** End-to-end Hardwood processing and materialization paths used by JMH and tests. */
public final class HardwoodMaterializationCases {

    private HardwoodMaterializationCases() {}

    /** Reads and checksums every projected field without retaining a secondary representation. */
    public static long hardwoodDirectProcessing(Path parquetFile, int batchSize) throws IOException {
        return processDirect(parquetFile, batchSize, null);
    }

    /** Uses the generated ranged-batch loader to build and seal a columnar store. */
    public static ProjectionStore<HardwoodMarketDataProjection> hardwoodToColumnarBatch(
            Path parquetFile, int rowCount, int batchSize) throws IOException {
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(parquetFile));
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(batchSize)
                        .build()) {
            return HardwoodMarketDataProjectionHardwoodLoader.load(columns, rowCount);
        }
    }

    /** Adds one reusable batch-backed view per row to the generated columnar store. */
    public static ProjectionStore<HardwoodMarketDataProjection> hardwoodToColumnarPerRow(
            Path parquetFile, int rowCount, int batchSize) throws IOException {
        ProjectionStore<HardwoodMarketDataProjection> store =
                new HardwoodMarketDataProjection__ColumnarProjectionStore(rowCount);
        BatchProjectionView view = new BatchProjectionView();
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(parquetFile));
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(batchSize)
                        .build()) {
            while (columns.nextBatch()) {
                int recordCount = columns.getRecordCount();
                view.use(columns);
                for (int rowIndex = 0; rowIndex < recordCount; rowIndex++) {
                    view.moveTo(rowIndex);
                    store.add(view);
                }
            }
        }
        store.seal();
        return store;
    }

    /** Creates one immutable row per decoded record and retains it in a pre-sized ArrayList. */
    public static ArrayList<HardwoodMarketDataRow> hardwoodToArrayList(
            Path parquetFile, int rowCount, int batchSize) throws IOException {
        ArrayList<HardwoodMarketDataRow> rows = new ArrayList<>(rowCount);
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(parquetFile));
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(batchSize)
                        .build()) {
            ColumnReader timestamp = columns.getColumnReader("timestamp");
            ColumnReader symbol = columns.getColumnReader("symbol");
            ColumnReader venue = columns.getColumnReader("venue");
            ColumnReader side = columns.getColumnReader("side");
            ColumnReader sequenceNumber = columns.getColumnReader("sequenceNumber");
            ColumnReader bidPrice = columns.getColumnReader("bidPrice");
            ColumnReader askPrice = columns.getColumnReader("askPrice");
            ColumnReader lastTradePrice = columns.getColumnReader("lastTradePrice");
            while (columns.nextBatch()) {
                int recordCount = columns.getRecordCount();
                long[] timestamps = timestamp.getLongs();
                String[] symbols = symbol.getStrings();
                String[] venues = venue.getStrings();
                String[] sides = side.getStrings();
                long[] sequenceNumbers = sequenceNumber.getLongs();
                double[] bidPrices = bidPrice.getDoubles();
                double[] askPrices = askPrice.getDoubles();
                double[] lastTradePrices = lastTradePrice.getDoubles();
                for (int rowIndex = 0; rowIndex < recordCount; rowIndex++) {
                    rows.add(new HardwoodMarketDataRow(
                            timestamps[rowIndex],
                            symbols[rowIndex],
                            venues[rowIndex],
                            sides[rowIndex],
                            sequenceNumbers[rowIndex],
                            bidPrices[rowIndex],
                            askPrices[rowIndex],
                            lastTradePrices[rowIndex]));
                }
            }
        }
        return rows;
    }

    static DirectValidation validateDirect(Path parquetFile, int batchSize) throws IOException {
        int[] counts = new int[2];
        long checksum = processDirect(parquetFile, batchSize, rows -> {
            counts[0]++;
            counts[1] += rows;
        });
        return new DirectValidation(checksum, counts[1], counts[0]);
    }

    static long checksum(ProjectionStore<HardwoodMarketDataProjection> store) {
        Checksum checksum = new Checksum();
        ProjectionCursor<HardwoodMarketDataProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            checksum.add(cursor.current());
        }
        return checksum.value();
    }

    static long checksum(List<? extends HardwoodMarketDataProjection> rows) {
        Checksum checksum = new Checksum();
        for (HardwoodMarketDataProjection row : rows) {
            checksum.add(row);
        }
        return checksum.value();
    }

    static long checksumExpectedRows(int rowCount) {
        Checksum checksum = new Checksum();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            checksum.add(HardwoodParquetDatasetGenerator.rowAt(rowIndex));
        }
        return checksum.value();
    }

    private static long processDirect(Path parquetFile, int batchSize, IntConsumer batchObserver)
            throws IOException {
        Checksum checksum = new Checksum();
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(parquetFile));
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(batchSize)
                        .build()) {
            ColumnReader timestamp = columns.getColumnReader("timestamp");
            ColumnReader symbol = columns.getColumnReader("symbol");
            ColumnReader venue = columns.getColumnReader("venue");
            ColumnReader side = columns.getColumnReader("side");
            ColumnReader sequenceNumber = columns.getColumnReader("sequenceNumber");
            ColumnReader bidPrice = columns.getColumnReader("bidPrice");
            ColumnReader askPrice = columns.getColumnReader("askPrice");
            ColumnReader lastTradePrice = columns.getColumnReader("lastTradePrice");
            while (columns.nextBatch()) {
                int recordCount = columns.getRecordCount();
                if (batchObserver != null) {
                    batchObserver.accept(recordCount);
                }
                long[] timestamps = timestamp.getLongs();
                String[] symbols = symbol.getStrings();
                String[] venues = venue.getStrings();
                String[] sides = side.getStrings();
                long[] sequenceNumbers = sequenceNumber.getLongs();
                double[] bidPrices = bidPrice.getDoubles();
                double[] askPrices = askPrice.getDoubles();
                double[] lastTradePrices = lastTradePrice.getDoubles();
                for (int rowIndex = 0; rowIndex < recordCount; rowIndex++) {
                    checksum.add(
                            timestamps[rowIndex],
                            symbols[rowIndex],
                            venues[rowIndex],
                            sides[rowIndex],
                            sequenceNumbers[rowIndex],
                            bidPrices[rowIndex],
                            askPrices[rowIndex],
                            lastTradePrices[rowIndex]);
                }
            }
        }
        return checksum.value();
    }

    record DirectValidation(long checksum, int rowCount, int batchCount) {}

    private static final class Checksum {

        private long checksum;
        private long rowCount;

        void add(HardwoodMarketDataProjection row) {
            add(
                    row.timestamp(),
                    row.symbol(),
                    row.venue(),
                    row.side(),
                    row.sequenceNumber(),
                    row.bidPrice(),
                    row.askPrice(),
                    row.lastTradePrice());
        }

        void add(
                long timestamp,
                String symbol,
                String venue,
                String side,
                long sequenceNumber,
                double bidPrice,
                double askPrice,
                double lastTradePrice) {
            long row = mix(timestamp, symbol.hashCode());
            row = mix(row, venue.hashCode());
            row = mix(row, side.hashCode());
            row = mix(row, sequenceNumber);
            row = mix(row, Double.doubleToLongBits(bidPrice));
            row = mix(row, Double.doubleToLongBits(askPrice));
            row = mix(row, Double.doubleToLongBits(lastTradePrice));
            checksum = mix(checksum, row);
            rowCount++;
        }

        long value() {
            return mix(checksum, rowCount);
        }

        private static long mix(long left, long right) {
            long value = left ^ (right + 0x9E3779B97F4A7C15L + (left << 6) + (left >>> 2));
            value ^= value >>> 30;
            value *= 0xBF58476D1CE4E5B9L;
            value ^= value >>> 27;
            value *= 0x94D049BB133111EBL;
            return value ^ (value >>> 31);
        }
    }

    private static final class BatchProjectionView implements HardwoodMarketDataProjection {

        private int rowIndex;
        private long[] timestamps;
        private String[] symbols;
        private String[] venues;
        private String[] sides;
        private long[] sequenceNumbers;
        private double[] bidPrices;
        private double[] askPrices;
        private double[] lastTradePrices;

        void use(ColumnReaders columns) {
            timestamps = columns.getColumnReader("timestamp").getLongs();
            symbols = columns.getColumnReader("symbol").getStrings();
            venues = columns.getColumnReader("venue").getStrings();
            sides = columns.getColumnReader("side").getStrings();
            sequenceNumbers = columns.getColumnReader("sequenceNumber").getLongs();
            bidPrices = columns.getColumnReader("bidPrice").getDoubles();
            askPrices = columns.getColumnReader("askPrice").getDoubles();
            lastTradePrices = columns.getColumnReader("lastTradePrice").getDoubles();
        }

        void moveTo(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        @Override
        public long timestamp() {
            return timestamps[rowIndex];
        }

        @Override
        public String symbol() {
            return symbols[rowIndex];
        }

        @Override
        public String venue() {
            return venues[rowIndex];
        }

        @Override
        public String side() {
            return sides[rowIndex];
        }

        @Override
        public long sequenceNumber() {
            return sequenceNumbers[rowIndex];
        }

        @Override
        public double bidPrice() {
            return bidPrices[rowIndex];
        }

        @Override
        public double askPrice() {
            return askPrices[rowIndex];
        }

        @Override
        public double lastTradePrice() {
            return lastTradePrices[rowIndex];
        }
    }
}
