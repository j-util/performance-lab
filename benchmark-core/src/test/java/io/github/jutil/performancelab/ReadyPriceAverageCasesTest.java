package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.junit.jupiter.api.Test;

import io.github.jutil.columnarprojection.ProjectionCursor;
import io.github.jutil.columnarprojection.ProjectionStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import tech.tablesaw.api.Table;

class ReadyPriceAverageCasesTest {

    @Test
    void generationIsDeterministicByRowIndex() {
        PriceTick first = PriceTickFixtures.tickAt(0);
        PriceTick sameFirst = PriceTickFixtures.tickAt(0);
        PriceTick second = PriceTickFixtures.tickAt(1);

        assertEquals(new PriceTick(1_704_067_200_000L, 1.99d), first);
        assertEquals(first, sameFirst);
        assertNotEquals(first.timestamp(), second.timestamp());
        assertNotEquals(first.price(), second.price());
        assertThrows(IllegalArgumentException.class, () -> PriceTickFixtures.tickAt(-1));
    }

    @Test
    void allRepresentationsProduceEquivalentAverageForSingleRecord() {
        assertEquivalentRepresentations(1);
    }

    @Test
    void allRepresentationsContainTheSameCompleteRecordsAndEquivalentAverages() {
        assertEquivalentRepresentations(37);
    }

    @Test
    void rejectsNonPositiveRowCounts() {
        for (int invalidRowCount : new int[] {0, -1}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyPriceAverageCases.newArrayList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyPriceAverageCases.newFastUtilObjectArrayList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyPriceAverageCases.newEclipseFastList(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyPriceAverageCases.newTablesawTable(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> ReadyPriceAverageCases.newColumnarProjectionStore(invalidRowCount));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PriceTickFixtures.expectedAverage(invalidRowCount));
        }
    }

    private static void assertEquivalentRepresentations(int rowCount) {
        List<PriceTick> expectedRows = new ArrayList<>(rowCount);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            expectedRows.add(PriceTickFixtures.tickAt(rowIndex));
        }

        ArrayList<PriceTick> arrayList = ReadyPriceAverageCases.newArrayList(rowCount);
        ObjectArrayList<PriceTick> fastUtil =
                ReadyPriceAverageCases.newFastUtilObjectArrayList(rowCount);
        FastList<PriceTick> eclipse = ReadyPriceAverageCases.newEclipseFastList(rowCount);
        Table tablesaw = ReadyPriceAverageCases.newTablesawTable(rowCount);
        ProjectionStore<PriceTickProjection> columnar =
                ReadyPriceAverageCases.newColumnarProjectionStore(rowCount);

        assertEquals(expectedRows, arrayList);
        assertEquals(expectedRows, new ArrayList<>(fastUtil));
        assertEquals(expectedRows, new ArrayList<>(eclipse));
        assertEquals(expectedRows, snapshot(tablesaw));
        assertEquals(expectedRows, snapshot(columnar));

        double expectedAverage = PriceTickFixtures.expectedAverage(rowCount);
        double tolerance = ReadyPriceAverageCases.toleranceFor(expectedAverage);
        assertEquals(
                expectedAverage,
                ReadyPriceAverageCases.arrayListPriceAverage(arrayList),
                tolerance);
        assertEquals(
                expectedAverage,
                ReadyPriceAverageCases.fastUtilObjectArrayListPriceAverage(fastUtil),
                tolerance);
        assertEquals(
                expectedAverage,
                ReadyPriceAverageCases.eclipseFastListPriceAverage(eclipse),
                tolerance);
        assertEquals(
                expectedAverage,
                ReadyPriceAverageCases.tablesawTablePriceAverage(tablesaw),
                tolerance);
        assertEquals(
                expectedAverage,
                ReadyPriceAverageCases.columnarProjectionStorePriceAverage(columnar),
                tolerance);
    }

    private static List<PriceTick> snapshot(Table table) {
        List<PriceTick> ticks = new ArrayList<>(table.rowCount());
        for (int rowIndex = 0; rowIndex < table.rowCount(); rowIndex++) {
            ticks.add(new PriceTick(
                    table.longColumn(ReadyPriceAverageCases.TIMESTAMP_COLUMN).getLong(rowIndex),
                    table.doubleColumn(ReadyPriceAverageCases.PRICE_COLUMN).getDouble(rowIndex)));
        }
        return ticks;
    }

    private static List<PriceTick> snapshot(ProjectionStore<PriceTickProjection> store) {
        List<PriceTick> ticks = new ArrayList<>(store.size());
        ProjectionCursor<PriceTickProjection> cursor = store.cursor();
        while (cursor.moveNext()) {
            PriceTickProjection tick = cursor.current();
            ticks.add(new PriceTick(tick.timestamp(), tick.price()));
        }
        return ticks;
    }
}
