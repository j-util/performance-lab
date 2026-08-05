package io.github.jutil.performancelab;

import java.util.ArrayList;

import org.eclipse.collections.impl.list.mutable.FastList;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.LongColumn;
import tech.tablesaw.api.Table;

/** Construction, calculation, and validation for ready-data price-average cases. */
final class ReadyPriceAverageCases {

    static final String TIMESTAMP_COLUMN = "timestamp";
    static final String PRICE_COLUMN = "price";

    private static final double RELATIVE_TOLERANCE = 1.0e-12d;
    private static final int ULP_TOLERANCE = 16;

    private ReadyPriceAverageCases() {
    }

    static ArrayList<PriceTick> newArrayList(int rowCount) {
        PriceTickFixtures.validateRowCount(rowCount);
        ArrayList<PriceTick> rows = new ArrayList<>(rowCount);
        addTicks(rowCount, rows::add);
        validateSize("ArrayList", rowCount, rows.size());
        return rows;
    }

    static ObjectArrayList<PriceTick> newFastUtilObjectArrayList(int rowCount) {
        PriceTickFixtures.validateRowCount(rowCount);
        ObjectArrayList<PriceTick> rows = new ObjectArrayList<>(rowCount);
        addTicks(rowCount, rows::add);
        validateSize("FastUtil ObjectArrayList", rowCount, rows.size());
        return rows;
    }

    static FastList<PriceTick> newEclipseFastList(int rowCount) {
        PriceTickFixtures.validateRowCount(rowCount);
        FastList<PriceTick> rows = new FastList<>(rowCount);
        addTicks(rowCount, rows::add);
        validateSize("Eclipse Collections FastList", rowCount, rows.size());
        return rows;
    }

    static Table newTablesawTable(int rowCount) {
        PriceTickFixtures.validateRowCount(rowCount);
        long[] timestamps = new long[rowCount];
        double[] prices = new double[rowCount];
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            PriceTick tick = PriceTickFixtures.tickAt(rowIndex);
            timestamps[rowIndex] = tick.timestamp();
            prices[rowIndex] = tick.price();
        }

        Table table = Table.create(
                "price ticks",
                LongColumn.create(TIMESTAMP_COLUMN, timestamps),
                DoubleColumn.create(PRICE_COLUMN, prices));
        validateSize("Tablesaw Table", rowCount, table.rowCount());
        if (table.columnCount() != 2) {
            throw new IllegalStateException(
                    "Tablesaw Table contains " + table.columnCount() + " columns; expected 2");
        }
        return table;
    }

    static ProjectionStore<PriceTickProjection> newColumnarProjectionStore(int rowCount) {
        PriceTickFixtures.validateRowCount(rowCount);
        ProjectionStore<PriceTickProjection> store =
                ProjectionStores.create(PriceTickProjection.class, rowCount);
        addTicks(rowCount, store::add);
        validateSize("Columnar Projection Store", rowCount, store.size());
        store.seal();
        return store;
    }

    static double arrayListPriceAverage(ArrayList<PriceTick> rows) {
        double sum = 0.0d;
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += rows.get(index).price();
        }
        return sum / rows.size();
    }

    static double fastUtilObjectArrayListPriceAverage(ObjectArrayList<PriceTick> rows) {
        double sum = 0.0d;
        Object[] elements = rows.elements();
        for (int index = 0, size = rows.size(); index < size; index++) {
            sum += ((PriceTick) elements[index]).price();
        }
        return sum / rows.size();
    }

    static double eclipseFastListPriceAverage(FastList<PriceTick> rows) {
        return rows.sumOfDouble(PriceTick::price) / rows.size();
    }

    static double tablesawTablePriceAverage(Table table) {
        return table.doubleColumn(PRICE_COLUMN).mean();
    }

    static double columnarProjectionStorePriceAverage(
            ProjectionStore<PriceTickProjection> store) {
        double sum = 0.0d;
        ProjectionCursor<PriceTickProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            sum += cursor.current().price();
        }
        return sum / store.size();
    }

    static void validateAverage(String representation, int rowCount, double actualAverage) {
        double expectedAverage = PriceTickFixtures.expectedAverage(rowCount);
        double tolerance = toleranceFor(expectedAverage);
        if (!Double.isFinite(actualAverage)
                || Math.abs(expectedAverage - actualAverage) > tolerance) {
            throw new IllegalStateException(
                    representation + " average was " + actualAverage + "; expected "
                            + expectedAverage + " within " + tolerance);
        }
    }

    static double toleranceFor(double expectedAverage) {
        return Math.max(
                Math.abs(expectedAverage) * RELATIVE_TOLERANCE,
                Math.ulp(expectedAverage) * ULP_TOLERANCE);
    }

    private static void addTicks(int rowCount, java.util.function.Consumer<PriceTick> destination) {
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            destination.accept(PriceTickFixtures.tickAt(rowIndex));
        }
    }

    private static void validateSize(String representation, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    representation + " contains " + actual + " rows; expected " + expected);
        }
    }
}
