package io.github.jutil.performancelab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.collections.impl.list.mutable.FastList;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;

/** Construction, calculation, and validation for maximum-by-double cases. */
final class MaxByDoubleCases {

    private static final Comparator<Position> MARKET_VALUE_COMPARATOR =
            Comparator.comparingDouble(Position::marketValue);

    private MaxByDoubleCases() {
    }

    static ArrayList<Position> newArrayList(int rowCount) {
        return newArrayList(MaxByDoubleFixtures.newPositions(rowCount));
    }

    static ArrayList<Position> newArrayList(Position[] positions) {
        validatePositions(positions);
        return new ArrayList<>(Arrays.asList(positions));
    }

    static FastList<Position> newEclipseFastList(int rowCount) {
        return newEclipseFastList(MaxByDoubleFixtures.newPositions(rowCount));
    }

    static FastList<Position> newEclipseFastList(Position[] positions) {
        validatePositions(positions);
        return FastList.newListWith(positions);
    }

    static ProjectionStore<PositionProjection> newColumnarProjectionStore(int rowCount) {
        return newColumnarProjectionStore(MaxByDoubleFixtures.newPositions(rowCount));
    }

    static ProjectionStore<PositionProjection> newColumnarProjectionStore(Position[] positions) {
        validatePositions(positions);
        ProjectionStore<PositionProjection> store =
                ProjectionStores.create(PositionProjection.class, positions.length);
        for (Position position : positions) {
            store.add(position);
        }
        validateSize("Columnar Projection Store", positions.length, store.size());
        store.seal();
        return store;
    }

    static ManualHybrid newManualHybrid(int rowCount) {
        return newManualHybrid(MaxByDoubleFixtures.newPositions(rowCount));
    }

    static ManualHybrid newManualHybrid(Position[] sourcePositions) {
        validatePositions(sourcePositions);
        Position[] positions = sourcePositions.clone();
        double[] marketValues = new double[positions.length];
        for (int index = 0; index < positions.length; index++) {
            marketValues[index] = positions[index].marketValue();
        }
        return new ManualHybrid(positions, marketValues);
    }

    static Position arrayListImperativeMaxByDouble(ArrayList<Position> positions) {
        Position winner = positions.getFirst();
        double maximum = winner.marketValue();
        for (int index = 1, size = positions.size(); index < size; index++) {
            Position candidate = positions.get(index);
            double candidateValue = candidate.marketValue();
            if (candidateValue > maximum) {
                maximum = candidateValue;
                winner = candidate;
            }
        }
        return winner;
    }

    static Position arrayListStreamMaxByDouble(ArrayList<Position> positions) {
        return positions.stream().max(MARKET_VALUE_COMPARATOR).orElseThrow();
    }

    static Position eclipseFastListMaxByDouble(FastList<Position> positions) {
        return positions.maxBy(Position::marketValue);
    }

    static Position columnarProjectionStoreMaxByDouble(
            ProjectionStore<PositionProjection> store) {
        ProjectionCursor<PositionProjection> cursor = store.cursor();
        if (!cursor.moveNext()) {
            throw new IllegalStateException("Columnar Projection Store must not be empty");
        }

        PositionProjection current = cursor.current();
        double maximum = current.marketValue();
        Position winner = current.original();
        while (cursor.moveNext()) {
            current = cursor.current();
            double candidateValue = current.marketValue();
            if (candidateValue > maximum) {
                maximum = candidateValue;
                winner = current.original();
            }
        }
        return winner;
    }

    static Position manualHybridMaxByDouble(ManualHybrid hybrid) {
        Position winner = hybrid.positions[0];
        double maximum = hybrid.marketValues[0];
        for (int index = 1; index < hybrid.marketValues.length; index++) {
            double candidateValue = hybrid.marketValues[index];
            if (candidateValue > maximum) {
                maximum = candidateValue;
                winner = hybrid.positions[index];
            }
        }
        return winner;
    }

    static void validateWinner(String representation, int rowCount, Position actual) {
        Position expected = MaxByDoubleFixtures.expectedWinner(rowCount);
        if (actual == null
                || actual.id() != expected.id()
                || Double.doubleToLongBits(actual.marketValue())
                        != Double.doubleToLongBits(expected.marketValue())) {
            throw new IllegalStateException(
                    representation + " winner was " + actual + "; expected ID " + expected.id()
                            + " with market value " + expected.marketValue());
        }
    }

    private static void validatePositions(Position[] positions) {
        if (positions.length == 0) {
            throw new IllegalArgumentException("Positions must not be empty");
        }
        for (int index = 0; index < positions.length; index++) {
            Position position = positions[index];
            if (position == null) {
                throw new IllegalArgumentException("Position at index " + index + " must not be null");
            }
            if (!Double.isFinite(position.marketValue())) {
                throw new IllegalArgumentException(
                        "Position at index " + index + " has a non-finite market value");
            }
        }
    }

    private static void validateSize(String representation, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    representation + " contains " + actual + " rows; expected " + expected);
        }
    }

    /** Complete position references paired with their precomputed market values. */
    static final class ManualHybrid {

        private final Position[] positions;
        private final double[] marketValues;

        private ManualHybrid(Position[] positions, double[] marketValues) {
            this.positions = positions;
            this.marketValues = marketValues;
        }

        Position[] positions() {
            return positions;
        }

        double[] marketValues() {
            return marketValues;
        }
    }
}
