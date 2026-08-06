package io.github.jutil.performancelab;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntFunction;

/** Fixed-width, row-oriented native storage for complete market-data snapshots. */
final class MemorySegmentMarketDataSnapshotStore implements AutoCloseable {

    /**
     * Symbols use one unsigned length byte followed by up to seven UTF-8 bytes. The remaining
     * bytes are zero-filled, making the eight-byte symbol field fixed-width and reversible.
     */
    static final int SYMBOL_UTF8_CAPACITY = 7;

    private static final MemoryLayout ROW_LAYOUT = MemoryLayout.structLayout(
            JAVA_LONG.withName("capturedAtNanos"),
            MemoryLayout.sequenceLayout(8, JAVA_BYTE).withName("symbol"),
            JAVA_DOUBLE.withName("lastTradePrice"),
            JAVA_DOUBLE.withName("lastTradeSize"),
            JAVA_DOUBLE.withName("bidPrice"),
            JAVA_DOUBLE.withName("askPrice"),
            JAVA_DOUBLE.withName("bidSize"),
            JAVA_DOUBLE.withName("askSize"));

    static final long ROW_ALIGNMENT = ROW_LAYOUT.byteAlignment();
    static final long ROW_STRIDE = ROW_LAYOUT.byteSize();

    private static final long CAPTURED_AT_NANOS_OFFSET = offset("capturedAtNanos");
    private static final long SYMBOL_LENGTH_OFFSET = offset("symbol");
    private static final long SYMBOL_BYTES_OFFSET = SYMBOL_LENGTH_OFFSET + 1L;
    private static final long LAST_TRADE_PRICE_OFFSET = offset("lastTradePrice");
    private static final long LAST_TRADE_SIZE_OFFSET = offset("lastTradeSize");
    private static final long BID_PRICE_OFFSET = offset("bidPrice");
    private static final long ASK_PRICE_OFFSET = offset("askPrice");
    private static final long BID_SIZE_OFFSET = offset("bidSize");
    private static final long ASK_SIZE_OFFSET = offset("askSize");

    private Arena arena;
    private final MemorySegment rows;
    private final int rowCount;

    private MemorySegmentMarketDataSnapshotStore(
            Arena arena, MemorySegment rows, int rowCount) {
        this.arena = arena;
        this.rows = rows;
        this.rowCount = rowCount;
    }

    static MemorySegmentMarketDataSnapshotStore fromFixture(int rowCount) {
        return fromSnapshots(rowCount, MarketDataSnapshotFixtures::snapshotAt);
    }

    static MemorySegmentMarketDataSnapshotStore fromSnapshots(
            int rowCount, IntFunction<MarketDataSnapshot> snapshotAt) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        Objects.requireNonNull(snapshotAt, "snapshotAt");
        long byteSize = Math.multiplyExact((long) rowCount, ROW_STRIDE);
        Arena arena = Arena.ofConfined();
        try {
            MemorySegment rows = arena.allocate(byteSize, ROW_ALIGNMENT);
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                write(rows, rowIndex, snapshotAt.apply(rowIndex));
            }
            return new MemorySegmentMarketDataSnapshotStore(arena, rows, rowCount);
        } catch (Throwable failure) {
            arena.close();
            throw failure;
        }
    }

    int rowCount() {
        return rowCount;
    }

    double lastTradePriceAverage() {
        double sum = 0.0d;
        long rowOffset = LAST_TRADE_PRICE_OFFSET;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            sum += rows.get(JAVA_DOUBLE, rowOffset);
            rowOffset += ROW_STRIDE;
        }
        return sum / rowCount;
    }

    MarketDataSnapshot snapshotAt(int rowIndex) {
        long rowOffset = checkedRowOffset(rowIndex);
        return new MarketDataSnapshot(
                rows.get(JAVA_LONG, rowOffset + CAPTURED_AT_NANOS_OFFSET),
                readSymbol(rows, rowOffset),
                rows.get(JAVA_DOUBLE, rowOffset + LAST_TRADE_PRICE_OFFSET),
                rows.get(JAVA_DOUBLE, rowOffset + LAST_TRADE_SIZE_OFFSET),
                rows.get(JAVA_DOUBLE, rowOffset + BID_PRICE_OFFSET),
                rows.get(JAVA_DOUBLE, rowOffset + ASK_PRICE_OFFSET),
                rows.get(JAVA_DOUBLE, rowOffset + BID_SIZE_OFFSET),
                rows.get(JAVA_DOUBLE, rowOffset + ASK_SIZE_OFFSET));
    }

    @Override
    public void close() {
        Arena ownedArena = arena;
        if (ownedArena != null) {
            arena = null;
            ownedArena.close();
        }
    }

    private static void write(
            MemorySegment rows, int rowIndex, MarketDataSnapshot snapshot) {
        long rowOffset = (long) rowIndex * ROW_STRIDE;
        byte[] symbol = encodedSymbol(snapshot.symbol());
        rows.set(JAVA_LONG, rowOffset + CAPTURED_AT_NANOS_OFFSET, snapshot.capturedAtNanos());
        rows.set(JAVA_BYTE, rowOffset + SYMBOL_LENGTH_OFFSET, (byte) symbol.length);
        MemorySegment.copy(
                MemorySegment.ofArray(symbol),
                0L,
                rows,
                rowOffset + SYMBOL_BYTES_OFFSET,
                symbol.length);
        rows.set(JAVA_DOUBLE, rowOffset + LAST_TRADE_PRICE_OFFSET, snapshot.lastTradePrice());
        rows.set(JAVA_DOUBLE, rowOffset + LAST_TRADE_SIZE_OFFSET, snapshot.lastTradeSize());
        rows.set(JAVA_DOUBLE, rowOffset + BID_PRICE_OFFSET, snapshot.bidPrice());
        rows.set(JAVA_DOUBLE, rowOffset + ASK_PRICE_OFFSET, snapshot.askPrice());
        rows.set(JAVA_DOUBLE, rowOffset + BID_SIZE_OFFSET, snapshot.bidSize());
        rows.set(JAVA_DOUBLE, rowOffset + ASK_SIZE_OFFSET, snapshot.askSize());
    }

    private static byte[] encodedSymbol(String symbol) {
        if (symbol == null) {
            throw new NullPointerException("Symbol must not be null");
        }
        byte[] encoded = symbol.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > SYMBOL_UTF8_CAPACITY) {
            throw new IllegalArgumentException(
                    "Symbol encodes to " + encoded.length + " UTF-8 bytes; maximum is "
                            + SYMBOL_UTF8_CAPACITY);
        }
        return encoded;
    }

    private static String readSymbol(MemorySegment rows, long rowOffset) {
        int length = Byte.toUnsignedInt(rows.get(JAVA_BYTE, rowOffset + SYMBOL_LENGTH_OFFSET));
        if (length > SYMBOL_UTF8_CAPACITY) {
            throw new IllegalStateException("Invalid stored symbol length: " + length);
        }
        return new String(
                rows.asSlice(rowOffset + SYMBOL_BYTES_OFFSET, length).toArray(JAVA_BYTE),
                StandardCharsets.UTF_8);
    }

    private long checkedRowOffset(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException(
                    "Row index " + rowIndex + " outside [0, " + rowCount + ')');
        }
        return (long) rowIndex * ROW_STRIDE;
    }

    private static long offset(String fieldName) {
        return ROW_LAYOUT.byteOffset(MemoryLayout.PathElement.groupElement(fieldName));
    }
}
