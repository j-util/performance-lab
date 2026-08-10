package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.hardwood.InputFile;
import dev.hardwood.reader.ColumnReader;
import dev.hardwood.reader.ColumnReaders;
import dev.hardwood.reader.ParquetFileReader;
import io.github.jutil.columnarprojection.ProjectionStore;

/** End-to-end Hardwood materialization paths used by JMH and tests. */
public final class HardwoodMaterializationCases {

    private HardwoodMaterializationCases() {}

    /** Uses the generated ranged-batch loader to build and seal a columnar store. */
    public static ProjectionStore<HardwoodMarketDataProjection> hardwoodToColumnarBatch(
            List<Path> parquetFiles) throws IOException {
        try (ParquetFileReader reader = openAll(parquetFiles)) {
            return HardwoodMarketDataProjectionHardwoodLoader.load(reader);
        }
    }

    /** Creates one immutable row per decoded record and retains it in an ArrayList. */
    public static ArrayList<HardwoodMarketDataRow> hardwoodToArrayList(
            List<Path> parquetFiles) throws IOException {
        try (ParquetFileReader reader = openAll(parquetFiles)) {
            int firstFileRowCount = Math.toIntExact(reader.getFileMetaData().numRows());
            ArrayList<HardwoodMarketDataRow> rows = new ArrayList<>(firstFileRowCount);
            try (ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
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
    }

    private static ParquetFileReader openAll(List<Path> parquetFiles) throws IOException {
        if (parquetFiles.size() != 2) {
            throw new IllegalArgumentException("Exactly two Parquet files are required");
        }
        return ParquetFileReader.openAll(List.of(
                InputFile.of(parquetFiles.get(0)), InputFile.of(parquetFiles.get(1))));
    }
}
