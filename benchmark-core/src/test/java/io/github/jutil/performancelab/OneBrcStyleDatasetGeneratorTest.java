package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OneBrcStyleDatasetGeneratorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatesDeterministicHeaderlessUtf8MeasurementsWithOneDecimalDigit() throws Exception {
        Path first = temporaryDirectory.resolve("first.csv");
        Path second = temporaryDirectory.resolve("nested/second.csv");

        OneBrcStyleDatasetGenerator.generate(100, first);
        OneBrcStyleDatasetGenerator.generate(100, second);

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
        try (CSVParser parser = CSVParser.parse(
                first,
                StandardCharsets.UTF_8,
                OneBrcStyleCsvParser.csvFormat())) {
            List<CSVRecord> records = parser.getRecords();
            assertEquals(100, records.size());
            Set<String> stations = new HashSet<>();
            for (CSVRecord record : records) {
                assertEquals(2, record.size());
                assertTrue(record.get(1).matches("-?\\d+\\.\\d"));
                stations.add(record.get(0));
            }
            assertTrue(stations.size() > 1);
            assertTrue(stations.size() < records.size());
            assertFalse(records.get(0).get(0).equals("station-name"));
        }
    }

    @Test
    void commandReusesAnExistingDatasetWithTheRequestedRowCount() throws Exception {
        Path output = temporaryDirectory.resolve("reusable.csv");
        OneBrcStyleDatasetGenerator.generate(7, output);
        byte[] original = Files.readAllBytes(output);
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();

        int exitCode = OneBrcStyleDatasetGenerator.run(
                new String[] {"7", output.toString()},
                new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        assertEquals(0, exitCode);
        assertTrue(standardOutput.toString(StandardCharsets.UTF_8).contains("Reusing 7 rows"));
        assertArrayEquals(original, Files.readAllBytes(output));
    }
}
