package io.github.jutil.performancelab;

import java.util.Objects;
import java.util.function.IntFunction;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.values.Values;

/** Typed fixed-size Chronicle Values rows backed by direct Chronicle Bytes. */
final class ChronicleValuesBytesMarketDataSnapshotStore implements AutoCloseable {

    private Bytes<Void> bytes;
    private final ChronicleMarketDataSnapshotValue row;
    private final long rowStride;
    private final int rowCount;

    private ChronicleValuesBytesMarketDataSnapshotStore(
            Bytes<Void> bytes,
            ChronicleMarketDataSnapshotValue row,
            long rowStride,
            int rowCount) {
        this.bytes = bytes;
        this.row = row;
        this.rowStride = rowStride;
        this.rowCount = rowCount;
    }

    static ChronicleValuesBytesMarketDataSnapshotStore fromFixture(int rowCount) {
        return fromSnapshots(rowCount, MarketDataSnapshotFixtures::snapshotAt);
    }

    static ChronicleValuesBytesMarketDataSnapshotStore fromSnapshots(
            int rowCount, IntFunction<MarketDataSnapshot> snapshotAt) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        Objects.requireNonNull(snapshotAt, "snapshotAt");
        ChronicleMarketDataSnapshotValue row =
                Values.newNativeReference(ChronicleMarketDataSnapshotValue.class);
        long rowStride = row.maxSize();
        long byteSize = Math.multiplyExact((long) rowCount, rowStride);
        Bytes<Void> bytes = Bytes.allocateDirect(byteSize);
        try {
            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                bind(row, bytes, rowStride, rowIndex);
                write(row, snapshotAt.apply(rowIndex));
            }
            bytes.writePosition(byteSize);
            bytes.readLimit(byteSize);
            return new ChronicleValuesBytesMarketDataSnapshotStore(
                    bytes, row, rowStride, rowCount);
        } catch (Throwable failure) {
            bytes.releaseLast();
            throw failure;
        }
    }

    int rowCount() {
        return rowCount;
    }

    long rowStride() {
        return rowStride;
    }

    boolean isDirectMemory() {
        return bytes.isDirectMemory();
    }

    double lastTradePriceAverage() {
        double sum = 0.0d;
        Bytes<Void> ownedBytes = bytes;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            bind(row, ownedBytes, rowStride, rowIndex);
            sum += row.getLastTradePrice();
        }
        return sum / rowCount;
    }

    MarketDataSnapshot snapshotAt(int rowIndex) {
        checkRowIndex(rowIndex);
        bind(row, bytes, rowStride, rowIndex);
        return new MarketDataSnapshot(
                row.getCapturedAtNanos(),
                row.getSymbol().toString(),
                row.getLastTradePrice(),
                row.getLastTradeSize(),
                row.getBidPrice(),
                row.getAskPrice(),
                row.getBidSize(),
                row.getAskSize());
    }

    @Override
    public void close() {
        Bytes<Void> ownedBytes = bytes;
        if (ownedBytes != null) {
            bytes = null;
            ownedBytes.releaseLast();
        }
    }

    private static void bind(
            ChronicleMarketDataSnapshotValue row,
            Bytes<Void> bytes,
            long rowStride,
            int rowIndex) {
        row.bytesStore(bytes, (long) rowIndex * rowStride, rowStride);
    }

    private static void write(
            ChronicleMarketDataSnapshotValue row, MarketDataSnapshot snapshot) {
        row.setCapturedAtNanos(snapshot.capturedAtNanos());
        row.setSymbol(snapshot.symbol());
        row.setLastTradePrice(snapshot.lastTradePrice());
        row.setLastTradeSize(snapshot.lastTradeSize());
        row.setBidPrice(snapshot.bidPrice());
        row.setAskPrice(snapshot.askPrice());
        row.setBidSize(snapshot.bidSize());
        row.setAskSize(snapshot.askSize());
    }

    private void checkRowIndex(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException(
                    "Row index " + rowIndex + " outside [0, " + rowCount + ')');
        }
    }
}
