package io.github.jutil.performancelab;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntFunction;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;

/** Complete off-heap Apache Arrow columnar storage for market-data snapshots. */
final class ApacheArrowMarketDataSnapshotStore implements AutoCloseable {

    private static final int SYMBOL_UTF8_CAPACITY = 7;

    private RootAllocator allocator;
    private VectorSchemaRoot root;
    private final BigIntVector capturedAtNanos;
    private final VarCharVector symbol;
    private final Float8Vector lastTradePrice;
    private final Float8Vector lastTradeSize;
    private final Float8Vector bidPrice;
    private final Float8Vector askPrice;
    private final Float8Vector bidSize;
    private final Float8Vector askSize;
    private final int rowCount;

    private ApacheArrowMarketDataSnapshotStore(
            RootAllocator allocator,
            VectorSchemaRoot root,
            BigIntVector capturedAtNanos,
            VarCharVector symbol,
            Float8Vector lastTradePrice,
            Float8Vector lastTradeSize,
            Float8Vector bidPrice,
            Float8Vector askPrice,
            Float8Vector bidSize,
            Float8Vector askSize,
            int rowCount) {
        this.allocator = allocator;
        this.root = root;
        this.capturedAtNanos = capturedAtNanos;
        this.symbol = symbol;
        this.lastTradePrice = lastTradePrice;
        this.lastTradeSize = lastTradeSize;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.bidSize = bidSize;
        this.askSize = askSize;
        this.rowCount = rowCount;
    }

    static ApacheArrowMarketDataSnapshotStore fromFixture(int rowCount) {
        return fromSnapshots(rowCount, MarketDataSnapshotFixtures::snapshotAt);
    }

    static ApacheArrowMarketDataSnapshotStore fromSnapshots(
            int rowCount, IntFunction<MarketDataSnapshot> snapshotAt) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        Objects.requireNonNull(snapshotAt, "snapshotAt");
        RootAllocator allocator = new RootAllocator();
        VectorSchemaRoot root = null;
        try {
            BigIntVector capturedAtNanos = new BigIntVector("capturedAtNanos", allocator);
            VarCharVector symbol = new VarCharVector("symbol", allocator);
            Float8Vector lastTradePrice = new Float8Vector("lastTradePrice", allocator);
            Float8Vector lastTradeSize = new Float8Vector("lastTradeSize", allocator);
            Float8Vector bidPrice = new Float8Vector("bidPrice", allocator);
            Float8Vector askPrice = new Float8Vector("askPrice", allocator);
            Float8Vector bidSize = new Float8Vector("bidSize", allocator);
            Float8Vector askSize = new Float8Vector("askSize", allocator);
            root = VectorSchemaRoot.of(
                    capturedAtNanos,
                    symbol,
                    lastTradePrice,
                    lastTradeSize,
                    bidPrice,
                    askPrice,
                    bidSize,
                    askSize);

            capturedAtNanos.allocateNew(rowCount);
            symbol.allocateNew(Math.multiplyExact(rowCount, SYMBOL_UTF8_CAPACITY), rowCount);
            lastTradePrice.allocateNew(rowCount);
            lastTradeSize.allocateNew(rowCount);
            bidPrice.allocateNew(rowCount);
            askPrice.allocateNew(rowCount);
            bidSize.allocateNew(rowCount);
            askSize.allocateNew(rowCount);

            for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                MarketDataSnapshot snapshot = snapshotAt.apply(rowIndex);
                byte[] encodedSymbol = encodedSymbol(snapshot.symbol());
                capturedAtNanos.set(rowIndex, snapshot.capturedAtNanos());
                symbol.set(rowIndex, encodedSymbol);
                lastTradePrice.set(rowIndex, snapshot.lastTradePrice());
                lastTradeSize.set(rowIndex, snapshot.lastTradeSize());
                bidPrice.set(rowIndex, snapshot.bidPrice());
                askPrice.set(rowIndex, snapshot.askPrice());
                bidSize.set(rowIndex, snapshot.bidSize());
                askSize.set(rowIndex, snapshot.askSize());
            }

            setValueCounts(
                    rowCount,
                    capturedAtNanos,
                    symbol,
                    lastTradePrice,
                    lastTradeSize,
                    bidPrice,
                    askPrice,
                    bidSize,
                    askSize);
            root.setRowCount(rowCount);
            validateValueCounts(root, rowCount);
            return new ApacheArrowMarketDataSnapshotStore(
                    allocator,
                    root,
                    capturedAtNanos,
                    symbol,
                    lastTradePrice,
                    lastTradeSize,
                    bidPrice,
                    askPrice,
                    bidSize,
                    askSize,
                    rowCount);
        } catch (Throwable failure) {
            if (root != null) {
                root.close();
            }
            allocator.close();
            throw failure;
        }
    }

    int rowCount() {
        return rowCount;
    }

    int columnCount() {
        return root.getFieldVectors().size();
    }

    double lastTradePriceAverage() {
        double sum = 0.0d;
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            sum += lastTradePrice.get(rowIndex);
        }
        return sum / rowCount;
    }

    MarketDataSnapshot snapshotAt(int rowIndex) {
        checkRowIndex(rowIndex);
        return new MarketDataSnapshot(
                capturedAtNanos.get(rowIndex),
                new String(symbol.get(rowIndex), StandardCharsets.UTF_8),
                lastTradePrice.get(rowIndex),
                lastTradeSize.get(rowIndex),
                bidPrice.get(rowIndex),
                askPrice.get(rowIndex),
                bidSize.get(rowIndex),
                askSize.get(rowIndex));
    }

    @Override
    public void close() {
        VectorSchemaRoot ownedRoot = root;
        RootAllocator ownedAllocator = allocator;
        root = null;
        allocator = null;
        try {
            if (ownedRoot != null) {
                ownedRoot.close();
            }
        } finally {
            if (ownedAllocator != null) {
                ownedAllocator.close();
            }
        }
    }

    private static byte[] encodedSymbol(String symbol) {
        byte[] encoded = symbol.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > SYMBOL_UTF8_CAPACITY) {
            throw new IllegalArgumentException(
                    "Symbol encodes to " + encoded.length + " UTF-8 bytes; maximum is "
                            + SYMBOL_UTF8_CAPACITY);
        }
        return encoded;
    }

    private static void setValueCounts(
            int rowCount,
            BigIntVector capturedAtNanos,
            VarCharVector symbol,
            Float8Vector lastTradePrice,
            Float8Vector lastTradeSize,
            Float8Vector bidPrice,
            Float8Vector askPrice,
            Float8Vector bidSize,
            Float8Vector askSize) {
        capturedAtNanos.setValueCount(rowCount);
        symbol.setValueCount(rowCount);
        lastTradePrice.setValueCount(rowCount);
        lastTradeSize.setValueCount(rowCount);
        bidPrice.setValueCount(rowCount);
        askPrice.setValueCount(rowCount);
        bidSize.setValueCount(rowCount);
        askSize.setValueCount(rowCount);
    }

    private static void validateValueCounts(VectorSchemaRoot root, int rowCount) {
        if (root.getFieldVectors().size() != 8) {
            throw new IllegalStateException(
                    "Apache Arrow root contains " + root.getFieldVectors().size()
                            + " columns; expected 8");
        }
        root.getFieldVectors().forEach(vector -> {
            if (vector.getValueCount() != rowCount) {
                throw new IllegalStateException(
                        vector.getName() + " contains " + vector.getValueCount()
                                + " values; expected " + rowCount);
            }
        });
    }

    private void checkRowIndex(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException(
                    "Row index " + rowIndex + " outside [0, " + rowCount + ')');
        }
    }
}
