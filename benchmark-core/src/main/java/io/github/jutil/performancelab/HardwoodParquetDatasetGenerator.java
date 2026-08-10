package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.DelegatingPositionOutputStream;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

/** Generates the deterministic uncompressed Parquet input used by Hardwood benchmarks. */
public final class HardwoodParquetDatasetGenerator {

    private static final String[] SYMBOLS = {"AAPL", "MSFT", "NVDA", "AMZN"};
    private static final String[] VENUES = {"XNAS", "XNYS", "ARCX"};
    private static final String[] SIDES = {"BID", "ASK"};
    private static final long BASE_TIMESTAMP = 1_700_000_000_000_000_000L;
    private static final long BASE_SEQUENCE_NUMBER = 10_000_000L;
    private static final MessageType SCHEMA = MessageTypeParser.parseMessageType("""
            message market_data {
              required int64 timestamp;
              required binary symbol (UTF8);
              required binary venue (UTF8);
              required binary side (UTF8);
              required int64 sequenceNumber;
              required double bidPrice;
              required double askPrice;
              required double lastTradePrice;
            }
            """);

    private HardwoodParquetDatasetGenerator() {}

    /** Writes exactly {@code rowCount} deterministic rows to a new file. */
    public static void write(Path path, int rowCount) throws IOException {
        if (rowCount < 0) {
            throw new IllegalArgumentException("rowCount must be greater than or equal to zero");
        }
        Path parent = path.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ParquetWriter<Group> writer = ExampleParquetWriter.builder(new NioOutputFile(path))
                .withType(SCHEMA)
                .withCompressionCodec(CompressionCodecName.UNCOMPRESSED)
                .withDictionaryEncoding(false)
                .withPageSize(1024 * 1024)
                .withRowGroupSize(64L * 1024L * 1024L)
                .build()) {
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                writer.write(group(rowIndex));
            }
        }
    }

    /** Creates and populates a temporary Parquet file for one JMH trial. */
    public static Path writeTemporary(int rowCount) throws IOException {
        Path path = Files.createTempFile("hardwood-market-data-" + rowCount + "-", ".parquet");
        Files.delete(path);
        try {
            write(path, rowCount);
            return path;
        } catch (IOException | RuntimeException | Error failure) {
            Files.deleteIfExists(path);
            throw failure;
        }
    }

    /** Returns the deterministic logical row at {@code rowIndex}. */
    static HardwoodMarketDataRow rowAt(int rowIndex) {
        if (rowIndex < 0) {
            throw new IllegalArgumentException("rowIndex must be greater than or equal to zero");
        }
        double bidPrice = 100.0D + (rowIndex % 1_000) * 0.125D;
        return new HardwoodMarketDataRow(
                BASE_TIMESTAMP + rowIndex,
                SYMBOLS[rowIndex % SYMBOLS.length],
                VENUES[rowIndex % VENUES.length],
                SIDES[rowIndex % SIDES.length],
                BASE_SEQUENCE_NUMBER + rowIndex,
                bidPrice,
                bidPrice + 0.25D,
                bidPrice + ((rowIndex % 3) - 1) * 0.125D);
    }

    private static Group group(int rowIndex) {
        HardwoodMarketDataRow row = rowAt(rowIndex);
        return new SimpleGroup(SCHEMA)
                .append("timestamp", row.timestamp())
                .append("symbol", row.symbol())
                .append("venue", row.venue())
                .append("side", row.side())
                .append("sequenceNumber", row.sequenceNumber())
                .append("bidPrice", row.bidPrice())
                .append("askPrice", row.askPrice())
                .append("lastTradePrice", row.lastTradePrice());
    }

    private record NioOutputFile(Path path) implements OutputFile {

        @Override
        public PositionOutputStream create(long blockSizeHint) throws IOException {
            return stream(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW));
        }

        @Override
        public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
            return stream(Files.newOutputStream(
                    path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
        }

        @Override
        public boolean supportsBlockSize() {
            return false;
        }

        @Override
        public long defaultBlockSize() {
            return 64L * 1024L * 1024L;
        }

        @Override
        public String getPath() {
            return path.toString();
        }

        private static PositionOutputStream stream(OutputStream output) {
            return new DelegatingPositionOutputStream(output) {
                private long position;

                @Override
                public long getPos() {
                    return position;
                }

                @Override
                public void write(int value) throws IOException {
                    super.write(value);
                    position++;
                }

                @Override
                public void write(byte[] bytes) throws IOException {
                    super.write(bytes);
                    position += bytes.length;
                }

                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    super.write(bytes, offset, length);
                    position += length;
                }
            };
        }
    }
}
