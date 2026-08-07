package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.jupiter.api.Test;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;

class MaxByDoubleCasesTest {

    @Test
    void fixtureGenerationIsDeterministicAndSharesProducts() {
        Position first = MaxByDoubleFixtures.positionAt(7, 37);
        Position repeated = MaxByDoubleFixtures.positionAt(7, 37);
        Position laterUseOfSameProduct = MaxByDoubleFixtures.positionAt(135, 200);

        assertEquals(first, repeated);
        assertSame(first.product(), repeated.product());
        assertSame(first.product(), laterUseOfSameProduct.product());
        assertNotEquals(first.id(), MaxByDoubleFixtures.positionAt(8, 37).id());
        assertTrue(Double.isFinite(first.marketValue()));
    }

    @Test
    void rejectsNonPositiveRowCounts() {
        for (int invalidRowCount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaxByDoubleFixtures.newPositions(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaxByDoubleCases.newArrayList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaxByDoubleCases.newEclipseFastList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaxByDoubleCases.newColumnarProjectionStore(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaxByDoubleCases.newManualHybrid(invalidRowCount));
        }
    }

    @Test
    void uniqueMaximumIsAwayFromDatasetEnds() {
        int rowCount = 101;
        Position[] positions = MaxByDoubleFixtures.newPositions(rowCount);
        int winningIndex = MaxByDoubleFixtures.winningIndex(rowCount);
        double maximum = positions[winningIndex].marketValue();
        int maximumCount = 0;

        assertNotEquals(0, winningIndex);
        assertNotEquals(rowCount - 1, winningIndex);
        for (Position position : positions) {
            if (Double.doubleToLongBits(position.marketValue())
                    == Double.doubleToLongBits(maximum)) {
                maximumCount++;
            }
        }
        assertEquals(1, maximumCount);
    }

    @Test
    void allRepresentationsReturnEquivalentWinnerForSingleRow() {
        assertEquivalentWinner(1);
    }

    @Test
    void allRepresentationsReturnEquivalentWinnerForRepresentativeSmallDatasets() {
        for (int rowCount : new int[] {3, 7, 37, 128}) {
            assertEquivalentWinner(rowCount);
        }
    }

    @Test
    void columnarStoreReturnsExactOriginallyAddedPositionReference() {
        Position[] originals = MaxByDoubleFixtures.newPositions(37);
        ProjectionStore<PositionProjection> store =
                MaxByDoubleCases.newColumnarProjectionStore(originals);
        ProjectionCursor<PositionProjection> cursor = store.cursor();
        int index = 0;
        while (cursor.moveNext()) {
            assertSame(originals[index], cursor.current().original());
            index++;
        }

        assertEquals(originals.length, index);
        assertSame(
                originals[MaxByDoubleFixtures.winningIndex(originals.length)],
                MaxByDoubleCases.columnarProjectionStoreMaxByDouble(store));
    }

    @Test
    void manualHybridRetainsAllPositionReferencesAndMatchingMarketValues() {
        Position[] originals = MaxByDoubleFixtures.newPositions(37);
        MaxByDoubleCases.ManualHybrid hybrid = MaxByDoubleCases.newManualHybrid(originals);

        assertEquals(originals.length, hybrid.positions().length);
        assertEquals(originals.length, hybrid.marketValues().length);
        for (int index = 0; index < originals.length; index++) {
            assertSame(originals[index], hybrid.positions()[index]);
            assertEquals(originals[index].marketValue(), hybrid.marketValues()[index]);
        }
    }

    private static void assertEquivalentWinner(int rowCount) {
        ArrayList<Position> arrayList = MaxByDoubleCases.newArrayList(rowCount);
        FastList<Position> eclipse = MaxByDoubleCases.newEclipseFastList(rowCount);
        ProjectionStore<PositionProjection> columnar =
                MaxByDoubleCases.newColumnarProjectionStore(rowCount);
        MaxByDoubleCases.ManualHybrid manual = MaxByDoubleCases.newManualHybrid(rowCount);
        Position expected = MaxByDoubleFixtures.expectedWinner(rowCount);

        assertEquivalent(expected, MaxByDoubleCases.arrayListImperativeMaxByDouble(arrayList));
        assertEquivalent(expected, MaxByDoubleCases.arrayListStreamMaxByDouble(arrayList));
        assertEquivalent(expected, MaxByDoubleCases.eclipseFastListMaxByDouble(eclipse));
        assertEquivalent(expected, MaxByDoubleCases.columnarProjectionStoreMaxByDouble(columnar));
        assertEquivalent(expected, MaxByDoubleCases.manualHybridMaxByDouble(manual));
    }

    private static void assertEquivalent(Position expected, Position actual) {
        assertEquals(expected.id(), actual.id());
        assertEquals(expected.marketValue(), actual.marketValue());
    }
}
