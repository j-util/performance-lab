package io.github.jutil.performancelab;

import java.util.ArrayList;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import io.github.jutil.columnarprojection.ProjectionStore;

/** Representation-specific state for the maximum-by-double benchmarks. */
public final class MaxByDoubleStateSupport {

    private MaxByDoubleStateSupport() {
    }

    /** Retains only the JDK ArrayList representation. */
    @State(Scope.Benchmark)
    public static class ArrayListState {

        @Param({"3000000"})
        public int rowCount;

        ArrayList<Position> positions;

        @Setup(Level.Trial)
        public void setup() {
            positions = MaxByDoubleCases.newArrayList(rowCount);
            MaxByDoubleCases.validateWinner(
                    "ArrayList imperative",
                    rowCount,
                    MaxByDoubleCases.arrayListImperativeMaxByDouble(positions));
            MaxByDoubleCases.validateWinner(
                    "ArrayList stream",
                    rowCount,
                    MaxByDoubleCases.arrayListStreamMaxByDouble(positions));
        }
    }

    /** Retains only the Eclipse Collections FastList representation. */
    @State(Scope.Benchmark)
    public static class EclipseFastListState {

        @Param({"3000000"})
        public int rowCount;

        FastList<Position> positions;

        @Setup(Level.Trial)
        public void setup() {
            positions = MaxByDoubleCases.newEclipseFastList(rowCount);
            MaxByDoubleCases.validateWinner(
                    "Eclipse Collections FastList",
                    rowCount,
                    MaxByDoubleCases.eclipseFastListMaxByDouble(positions));
        }
    }

    /** Retains only the Columnar Projection Store representation. */
    @State(Scope.Benchmark)
    public static class ColumnarProjectionStoreState {

        @Param({"3000000"})
        public int rowCount;

        ProjectionStore<PositionProjection> store;

        @Setup(Level.Trial)
        public void setup() {
            store = MaxByDoubleCases.newColumnarProjectionStore(rowCount);
            MaxByDoubleCases.validateWinner(
                    "Columnar Projection Store",
                    rowCount,
                    MaxByDoubleCases.columnarProjectionStoreMaxByDouble(store));
        }
    }

    /** Retains only the manual position-reference and market-value arrays. */
    @State(Scope.Benchmark)
    public static class ManualHybridState {

        @Param({"3000000"})
        public int rowCount;

        MaxByDoubleCases.ManualHybrid hybrid;

        @Setup(Level.Trial)
        public void setup() {
            hybrid = MaxByDoubleCases.newManualHybrid(rowCount);
            MaxByDoubleCases.validateWinner(
                    "Manual hybrid",
                    rowCount,
                    MaxByDoubleCases.manualHybridMaxByDouble(hybrid));
        }
    }
}
