package io.github.jutil.performancelab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

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
            Path parquetFile, int rowCount, int batchSize) throws IOException {
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(parquetFile));
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(batchSize)
                        .build()) {
            return HardwoodMarketDataProjectionHardwoodLoader.load(columns, rowCount);
        }
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
}
