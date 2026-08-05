package io.github.jutil.performancelab;

import java.util.ArrayList;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.github.jutil.columnarprojection.ProjectionStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tech.tablesaw.api.Table;

/** Representation-specific JMH state for ready-data price-average benchmarks. */
public final class ReadyPriceAverageStateSupport {

    private ReadyPriceAverageStateSupport() {
    }

    /** Retains only the JDK ArrayList representation. */
    @State(Scope.Benchmark)
    public static class ArrayListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ArrayList<PriceTick> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyPriceAverageCases.newArrayList(rowCount);
            ReadyPriceAverageCases.validateAverage(
                    "ArrayList", rowCount, ReadyPriceAverageCases.arrayListPriceAverage(rows));
        }
    }

    /** Retains only the FastUtil ObjectArrayList representation. */
    @State(Scope.Benchmark)
    public static class FastUtilObjectArrayListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ObjectArrayList<PriceTick> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyPriceAverageCases.newFastUtilObjectArrayList(rowCount);
            ReadyPriceAverageCases.validateAverage(
                    "FastUtil ObjectArrayList",
                    rowCount,
                    ReadyPriceAverageCases.fastUtilObjectArrayListPriceAverage(rows));
        }
    }

    /** Retains only the Eclipse Collections FastList representation. */
    @State(Scope.Benchmark)
    public static class EclipseFastListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        FastList<PriceTick> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyPriceAverageCases.newEclipseFastList(rowCount);
            ReadyPriceAverageCases.validateAverage(
                    "Eclipse Collections FastList",
                    rowCount,
                    ReadyPriceAverageCases.eclipseFastListPriceAverage(rows));
        }
    }

    /** Retains only the Tablesaw Table representation. */
    @State(Scope.Benchmark)
    public static class TablesawTableState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        Table table;

        @Setup(Level.Trial)
        public void setup() {
            table = ReadyPriceAverageCases.newTablesawTable(rowCount);
            ReadyPriceAverageCases.validateAverage(
                    "Tablesaw Table",
                    rowCount,
                    ReadyPriceAverageCases.tablesawTablePriceAverage(table));
        }
    }

    /** Retains only the parallel primitive timestamp and price arrays. */
    @State(Scope.Benchmark)
    public static class PrimitiveArraysState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        long[] timestamps;
        double[] prices;

        @Setup(Level.Trial)
        public void setup() {
            ReadyPriceAverageCases.PrimitiveArrays arrays =
                    ReadyPriceAverageCases.newPrimitiveArrays(rowCount);
            timestamps = arrays.timestamps();
            prices = arrays.prices();
            ReadyPriceAverageCases.validateAverage(
                    "Primitive arrays",
                    rowCount,
                    ReadyPriceAverageCases.primitiveArraysPriceAverage(prices));
        }
    }

    /** Retains only the Columnar Projection Store representation. */
    @State(Scope.Benchmark)
    public static class ColumnarProjectionStoreState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ProjectionStore<PriceTickProjection> store;

        @Setup(Level.Trial)
        public void setup() {
            store = ReadyPriceAverageCases.newColumnarProjectionStore(rowCount);
            ReadyPriceAverageCases.validateAverage(
                    "Columnar Projection Store",
                    rowCount,
                    ReadyPriceAverageCases.columnarProjectionStorePriceAverage(store));
        }
    }
}
