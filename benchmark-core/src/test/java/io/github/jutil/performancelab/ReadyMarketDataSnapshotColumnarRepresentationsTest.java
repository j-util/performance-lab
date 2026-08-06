package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dflib.DataFrame;
import org.dflib.DoubleSeries;
import org.junit.jupiter.api.Test;

class ReadyMarketDataSnapshotColumnarRepresentationsTest {

    private static final String[] EXPECTED_COLUMN_ORDER = {
        ReadyMarketDataSnapshotAverageCases.CAPTURED_AT_NANOS_COLUMN,
        ReadyMarketDataSnapshotAverageCases.SYMBOL_COLUMN,
        ReadyMarketDataSnapshotAverageCases.LAST_TRADE_PRICE_COLUMN,
        ReadyMarketDataSnapshotAverageCases.LAST_TRADE_SIZE_COLUMN,
        ReadyMarketDataSnapshotAverageCases.BID_PRICE_COLUMN,
        ReadyMarketDataSnapshotAverageCases.ASK_PRICE_COLUMN,
        ReadyMarketDataSnapshotAverageCases.BID_SIZE_COLUMN,
        ReadyMarketDataSnapshotAverageCases.ASK_SIZE_COLUMN
    };

    @Test
    void oneRowPreservesAndRecoversEveryFieldInOrder() {
        assertCompleteRepresentations(1);
    }

    @Test
    void multipleRowsPreserveAndRecoverEveryFieldInOrder() {
        assertCompleteRepresentations(37);
    }

    @Test
    void rejectsNonPositiveLogicalRowCounts() {
        for (int invalidRowCount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases
                            .newDflibDataFrame(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyMarketDataSnapshotAverageCases.newHppcColumns(invalidRowCount));
        }
    }

    @Test
    void calculationsReadTheLastTradePriceColumn() {
        int rowCount = 37;
        DataFrame dataFrame =
                ReadyMarketDataSnapshotAverageCases.newDflibDataFrame(rowCount);
        HppcMarketDataSnapshotColumns hppc =
                ReadyMarketDataSnapshotAverageCases.newHppcColumns(rowCount);

        double ordinaryAverage = ReadyMarketDataSnapshotAverageCases
                .arrayListLastTradePriceAverage(
                        ReadyMarketDataSnapshotAverageCases.newArrayList(rowCount));
        DoubleSeries lastTradePrices = dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_PRICE_COLUMN)
                .castAsDouble();
        double dflibNative = ReadyMarketDataSnapshotAverageCases
                .dflibDataFrameLastTradePriceAverage(dataFrame);
        double dflibNaive = ReadyMarketDataSnapshotAverageCases
                .dflibDataFrameNaiveLastTradePriceAverage(dataFrame);
        double hppcAverage = ReadyMarketDataSnapshotAverageCases
                .hppcColumnarLastTradePriceAverage(hppc);

        assertEquals(lastTradePrices.avg(), dflibNative);
        assertEquals(ordinaryAverage, dflibNaive);
        assertEquals(ordinaryAverage, hppcAverage);
        assertEquals(
                ordinaryAverage,
                dflibNative,
                ReadyMarketDataSnapshotAverageCases.toleranceFor(ordinaryAverage));
        assertNotEquals(
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.BID_PRICE_COLUMN)
                        .castAsDouble()
                        .avg(),
                dflibNative);
    }

    private static void assertCompleteRepresentations(int rowCount) {
        DataFrame dataFrame =
                ReadyMarketDataSnapshotAverageCases.newDflibDataFrame(rowCount);
        HppcMarketDataSnapshotColumns hppc =
                ReadyMarketDataSnapshotAverageCases.newHppcColumns(rowCount);

        assertEquals(rowCount, dataFrame.height());
        assertEquals(8, dataFrame.width());
        assertArrayEquals(EXPECTED_COLUMN_ORDER, dataFrame.getColumnsIndex().toArray());
        assertEquals(rowCount, hppc.rowCount());
        assertEquals(8, hppc.columnCount());

        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.CAPTURED_AT_NANOS_COLUMN)
                .castAsLong();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_PRICE_COLUMN)
                .castAsDouble();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_SIZE_COLUMN)
                .castAsDouble();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.BID_PRICE_COLUMN)
                .castAsDouble();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.ASK_PRICE_COLUMN)
                .castAsDouble();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.BID_SIZE_COLUMN)
                .castAsDouble();
        dataFrame
                .getColumn(ReadyMarketDataSnapshotAverageCases.ASK_SIZE_COLUMN)
                .castAsDouble();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            MarketDataSnapshot expected = MarketDataSnapshotFixtures.snapshotAt(rowIndex);
            assertEquals(expected, dflibSnapshotAt(dataFrame, rowIndex));
            assertEquals(expected, hppc.snapshotAt(rowIndex));
            assertEquals(
                    expected.symbol(),
                    dataFrame
                            .getColumn(ReadyMarketDataSnapshotAverageCases.SYMBOL_COLUMN)
                            .get(rowIndex));
        }
    }

    private static MarketDataSnapshot dflibSnapshotAt(DataFrame dataFrame, int rowIndex) {
        return new MarketDataSnapshot(
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.CAPTURED_AT_NANOS_COLUMN)
                        .castAsLong()
                        .getLong(rowIndex),
                (String) dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.SYMBOL_COLUMN)
                        .get(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_PRICE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.LAST_TRADE_SIZE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.BID_PRICE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.ASK_PRICE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.BID_SIZE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex),
                dataFrame
                        .getColumn(ReadyMarketDataSnapshotAverageCases.ASK_SIZE_COLUMN)
                        .castAsDouble()
                        .getDouble(rowIndex));
    }
}
