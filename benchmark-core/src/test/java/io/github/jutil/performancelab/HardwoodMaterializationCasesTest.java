package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.hardwood.InputFile;
import dev.hardwood.reader.ColumnReader;
import dev.hardwood.reader.ColumnReaders;
import dev.hardwood.reader.ParquetFileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.jutil.columnarprojection.ProjectionStore;

class HardwoodMaterializationCasesTest {

    private static final int OBSERVATION_BATCH_SIZE = 7;
    private static final int ROW_COUNT = OBSERVATION_BATCH_SIZE * 2 + 5;
    private static final int FIRST_FILE_ROW_COUNT = ROW_COUNT / 4;
    private static final int SECOND_FILE_ROW_COUNT = ROW_COUNT - FIRST_FILE_ROW_COUNT;

    @TempDir
    Path temporaryDirectory;

    @Test
    void fixtureIsTwoRealFilesAndHardwoodReadsThemAsOneOrderedInput() throws Exception {
        List<Path> parquetFiles = writeFixture("ordered");

        assertNotEquals(parquetFiles.get(0), parquetFiles.get(1));
        assertTrue(Files.isRegularFile(parquetFiles.get(0)));
        assertTrue(Files.isRegularFile(parquetFiles.get(1)));
        assertEquals(FIRST_FILE_ROW_COUNT, fileRowCount(parquetFiles.get(0)));
        assertEquals(SECOND_FILE_ROW_COUNT, fileRowCount(parquetFiles.get(1)));
        assertTrue(FIRST_FILE_ROW_COUNT < ROW_COUNT);

        try (ParquetFileReader reader = openAll(parquetFiles);
                ColumnReaders columns = reader
                        .buildColumnReaders(HardwoodMarketDataProjectionHardwoodLoader.projection())
                        .batchSize(OBSERVATION_BATCH_SIZE)
                        .build()) {
            assertTrue(reader.isMultiFile());
            assertEquals(2, reader.getFileCount());
            assertEquals(FIRST_FILE_ROW_COUNT, reader.getFileMetaData(0).numRows());
            assertEquals(SECOND_FILE_ROW_COUNT, reader.getFileMetaData(1).numRows());
            assertEquals(FIRST_FILE_ROW_COUNT, reader.getFileMetaData().numRows());

            ColumnReader sequenceNumber = columns.getColumnReader("sequenceNumber");
            ArrayList<Integer> batchSizes = new ArrayList<>();
            int globalRowIndex = 0;
            while (columns.nextBatch()) {
                int recordCount = columns.getRecordCount();
                batchSizes.add(recordCount);
                long[] sequenceNumbers = sequenceNumber.getLongs();
                for (int batchRowIndex = 0; batchRowIndex < recordCount; batchRowIndex++) {
                    assertEquals(
                            HardwoodParquetDatasetGenerator.rowAt(globalRowIndex).sequenceNumber(),
                            sequenceNumbers[batchRowIndex]);
                    globalRowIndex++;
                }
            }

            assertEquals(ROW_COUNT, globalRowIndex);
            assertEquals(List.of(FIRST_FILE_ROW_COUNT, 7, 7, 1), batchSizes);
        }
    }

    @Test
    void generatedLoaderAndArrayListMaterializeEquivalentOrderedRows() throws Exception {
        List<Path> parquetFiles = writeFixture("materialized");

        ProjectionStore<HardwoodMarketDataProjection> store =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(parquetFiles);
        ArrayList<HardwoodMarketDataRow> rows =
                HardwoodMaterializationCases.hardwoodToArrayList(parquetFiles);

        assertEquals(ROW_COUNT, store.size());
        assertEquals(ROW_COUNT, rows.size());
        assertInstanceOf(HardwoodMarketDataProjectionStore.class, store);
        assertEquals(ROW_COUNT, storeCapacity(store));
        assertSealed(store);

        for (int rowIndex = 0; rowIndex < ROW_COUNT; rowIndex++) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            assertProjectionEquals(expected, store.viewAt(rowIndex), "columnar row " + rowIndex);
            assertEquals(expected, rows.get(rowIndex), "ArrayList row " + rowIndex);
            assertProjectionEquals(rows.get(rowIndex), store.viewAt(rowIndex),
                    "destination equivalence at row " + rowIndex);
        }

        assertSelectedRows(store, rows, 0, FIRST_FILE_ROW_COUNT - 1, FIRST_FILE_ROW_COUNT,
                OBSERVATION_BATCH_SIZE - 1, OBSERVATION_BATCH_SIZE,
                OBSERVATION_BATCH_SIZE * 2 - 1, OBSERVATION_BATCH_SIZE * 2,
                ROW_COUNT - 1);
        assertStableStringReferences(store, rows, FIRST_FILE_ROW_COUNT);
    }

    @Test
    void repeatedInvocationsCreateIndependentDestinationsPositionedAtTheBeginning()
            throws Exception {
        List<Path> parquetFiles = writeFixture("repeated");

        ProjectionStore<HardwoodMarketDataProjection> firstStore =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(parquetFiles);
        ProjectionStore<HardwoodMarketDataProjection> secondStore =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(parquetFiles);
        ArrayList<HardwoodMarketDataRow> firstRows =
                HardwoodMaterializationCases.hardwoodToArrayList(parquetFiles);
        ArrayList<HardwoodMarketDataRow> secondRows =
                HardwoodMaterializationCases.hardwoodToArrayList(parquetFiles);

        assertNotSame(firstStore, secondStore);
        assertNotSame(firstRows, secondRows);
        assertProjectionEquals(HardwoodParquetDatasetGenerator.rowAt(0), firstStore.viewAt(0),
                "first columnar invocation");
        assertProjectionEquals(HardwoodParquetDatasetGenerator.rowAt(0), secondStore.viewAt(0),
                "second columnar invocation");
        assertEquals(HardwoodParquetDatasetGenerator.rowAt(0), firstRows.get(0));
        assertEquals(HardwoodParquetDatasetGenerator.rowAt(0), secondRows.get(0));

        firstRows.clear();
        assertEquals(ROW_COUNT, secondRows.size());
        assertEquals(ROW_COUNT, firstStore.size());
        assertEquals(ROW_COUNT, secondStore.size());
    }

    @Test
    void oneRowDatasetKeepsBothFilesValidAndUsesAllFileMetadata()
            throws Exception {
        int rowCount = 1;
        Path firstPath = temporaryDirectory.resolve("small-first.parquet");
        Path secondPath = temporaryDirectory.resolve("small-second.parquet");
        List<Path> parquetFiles = List.of(firstPath, secondPath);
        HardwoodParquetDatasetGenerator.write(firstPath, secondPath, rowCount);

        assertEquals(0, fileRowCount(firstPath));
        assertEquals(1, fileRowCount(secondPath));
        try (ParquetFileReader reader = openAll(parquetFiles)) {
            assertTrue(reader.isMultiFile());
            assertEquals(2, reader.getFileCount());
            assertEquals(0, reader.getFileMetaData(0).numRows());
            assertEquals(1, reader.getFileMetaData(1).numRows());
            assertEquals(0, reader.getFileMetaData().numRows());
        }

        ProjectionStore<HardwoodMarketDataProjection> store =
                HardwoodMaterializationCases.hardwoodToColumnarBatch(parquetFiles);
        ArrayList<HardwoodMarketDataRow> rows =
                HardwoodMaterializationCases.hardwoodToArrayList(parquetFiles);
        assertEquals(1, store.size());
        assertEquals(1, rows.size());
        assertEquals(1, storeCapacity(store));
        assertProjectionEquals(HardwoodParquetDatasetGenerator.rowAt(0), store.viewAt(0),
                "small columnar row");
        assertEquals(HardwoodParquetDatasetGenerator.rowAt(0), rows.get(0));
    }

    private List<Path> writeFixture(String prefix) throws Exception {
        Path firstPath = temporaryDirectory.resolve(prefix + "-first.parquet");
        Path secondPath = temporaryDirectory.resolve(prefix + "-second.parquet");
        HardwoodParquetDatasetGenerator.write(firstPath, secondPath, ROW_COUNT);
        return List.of(firstPath, secondPath);
    }

    private static long fileRowCount(Path path) throws Exception {
        try (ParquetFileReader reader = ParquetFileReader.open(InputFile.of(path))) {
            return reader.getFileMetaData().numRows();
        }
    }

    private static ParquetFileReader openAll(List<Path> parquetFiles) throws Exception {
        return ParquetFileReader.openAll(List.of(
                InputFile.of(parquetFiles.get(0)), InputFile.of(parquetFiles.get(1))));
    }

    private static int storeCapacity(ProjectionStore<HardwoodMarketDataProjection> store)
            throws Exception {
        Field capacity = store.getClass().getDeclaredField("capacity");
        capacity.setAccessible(true);
        return capacity.getInt(store);
    }

    private static void assertSealed(ProjectionStore<HardwoodMarketDataProjection> store) {
        store.cursor();
        assertThrows(
                IllegalStateException.class,
                () -> store.add(HardwoodParquetDatasetGenerator.rowAt(0)));
    }

    private static void assertSelectedRows(
            ProjectionStore<HardwoodMarketDataProjection> store,
            ArrayList<HardwoodMarketDataRow> rows,
            int... rowIndexes) {
        for (int rowIndex : rowIndexes) {
            HardwoodMarketDataRow expected = HardwoodParquetDatasetGenerator.rowAt(rowIndex);
            assertProjectionEquals(expected, store.viewAt(rowIndex), "selected columnar row " + rowIndex);
            assertEquals(expected, rows.get(rowIndex), "selected ArrayList row " + rowIndex);
        }
    }

    private static void assertStableStringReferences(
            ProjectionStore<HardwoodMarketDataProjection> store,
            ArrayList<HardwoodMarketDataRow> rows,
            int rowIndex) {
        HardwoodMarketDataProjection firstView = store.viewAt(rowIndex);
        HardwoodMarketDataProjection secondView = store.viewAt(rowIndex);
        assertSame(firstView.symbol(), secondView.symbol());
        assertSame(firstView.venue(), secondView.venue());
        assertSame(firstView.side(), secondView.side());

        HardwoodMarketDataRow row = rows.get(rowIndex);
        assertSame(row.symbol(), row.symbol());
        assertSame(row.venue(), row.venue());
        assertSame(row.side(), row.side());
    }

    private static void assertProjectionEquals(
            HardwoodMarketDataProjection expected,
            HardwoodMarketDataProjection actual,
            String context) {
        assertEquals(expected.timestamp(), actual.timestamp(), context + " timestamp");
        assertEquals(expected.symbol(), actual.symbol(), context + " symbol");
        assertEquals(expected.venue(), actual.venue(), context + " venue");
        assertEquals(expected.side(), actual.side(), context + " side");
        assertEquals(expected.sequenceNumber(), actual.sequenceNumber(), context + " sequenceNumber");
        assertEquals(expected.bidPrice(), actual.bidPrice(), context + " bidPrice");
        assertEquals(expected.askPrice(), actual.askPrice(), context + " askPrice");
        assertEquals(expected.lastTradePrice(), actual.lastTradePrice(), context + " lastTradePrice");
    }
}
