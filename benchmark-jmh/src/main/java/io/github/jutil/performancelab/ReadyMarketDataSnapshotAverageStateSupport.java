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

/** Representation-specific JMH state for ready market-data snapshot benchmarks. */
public final class ReadyMarketDataSnapshotAverageStateSupport {

    private ReadyMarketDataSnapshotAverageStateSupport() {
    }

    /** Retains only the JDK ArrayList representation. */
    @State(Scope.Benchmark)
    public static class ArrayListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ArrayList<MarketDataSnapshot> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyMarketDataSnapshotAverageCases.newArrayList(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "ArrayList",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases.arrayListLastTradePriceAverage(rows));
        }
    }

    /** Retains only the FastUtil ObjectArrayList representation. */
    @State(Scope.Benchmark)
    public static class FastUtilObjectArrayListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ObjectArrayList<MarketDataSnapshot> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyMarketDataSnapshotAverageCases.newFastUtilObjectArrayList(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "FastUtil ObjectArrayList",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases
                            .fastUtilObjectArrayListLastTradePriceAverage(rows));
        }
    }

    /** Retains only the Eclipse Collections FastList representation. */
    @State(Scope.Benchmark)
    public static class EclipseFastListState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        FastList<MarketDataSnapshot> rows;

        @Setup(Level.Trial)
        public void setup() {
            rows = ReadyMarketDataSnapshotAverageCases.newEclipseFastList(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "Eclipse Collections FastList",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases
                            .eclipseFastListLastTradePriceAverage(rows));
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
            table = ReadyMarketDataSnapshotAverageCases.newTablesawTable(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "Tablesaw Table",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases
                            .tablesawTableLastTradePriceAverage(table));
        }
    }

    /** Retains only the last-trade-price array used by the calculation baseline. */
    @State(Scope.Benchmark)
    public static class DoubleArrayBaselineState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        double[] lastTradePrices;

        @Setup(Level.Trial)
        public void setup() {
            lastTradePrices = ReadyMarketDataSnapshotAverageCases
                    .newDoubleArrayBaselineLastTradePrices(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "Double-array baseline",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases
                            .doubleArrayBaselineLastTradePriceAverage(lastTradePrices));
        }
    }

    /** Retains only the Columnar Projection Store representation. */
    @State(Scope.Benchmark)
    public static class ColumnarProjectionStoreState {

        @Param({"1000", "100000", "1000000", "10000000"})
        public int rowCount;

        ProjectionStore<MarketDataSnapshotProjection> store;

        @Setup(Level.Trial)
        public void setup() {
            store = ReadyMarketDataSnapshotAverageCases.newColumnarProjectionStore(rowCount);
            ReadyMarketDataSnapshotAverageCases.validateAverage(
                    "Columnar Projection Store",
                    rowCount,
                    ReadyMarketDataSnapshotAverageCases
                            .columnarProjectionStoreLastTradePriceAverage(store));
        }
    }
}
