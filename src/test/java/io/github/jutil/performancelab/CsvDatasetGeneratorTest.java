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
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvDatasetGeneratorTest {

    private static final List<String> EXPECTED_HEADER = List.of(
            "id", "customerId", "productId", "quantity", "priceCents", "timestamp", "region", "status");

    @TempDir
    Path temporaryDirectory;

    @Test
    void producesIdenticalBytesForTheSameRowCount() throws Exception {
        Path first = temporaryDirectory.resolve("first.csv");
        Path second = temporaryDirectory.resolve("nested/second.csv");

        CsvDatasetGenerator.generate(20, first);
        CsvDatasetGenerator.generate(20, second);

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
    }

    @Test
    void writesExpectedHeaderAndRowCount() throws Exception {
        Path output = temporaryDirectory.resolve("dataset.csv");
        CsvDatasetGenerator.generate(7, output);

        try (CSVParser parser = CSVParser.parse(
                output,
                StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get())) {
            assertEquals(EXPECTED_HEADER, parser.getHeaderNames());
            List<CSVRecord> records = parser.getRecords();
            assertEquals(7, records.size());
            assertEquals("1", records.get(0).get("id"));
            assertEquals("7", records.get(6).get("id"));
        }
    }

    @Test
    void rejectsInvalidArgumentsWithoutCreatingAFile() {
        Path output = temporaryDirectory.resolve("invalid.csv");
        String[][] invalidArguments = {
            {},
            {"not-a-number"},
            {"0"},
            {"-1"},
            {"1", output.toString(), "extra"},
            {"1", " "}
        };

        for (String[] arguments : invalidArguments) {
            ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
            ByteArrayOutputStream standardError = new ByteArrayOutputStream();

            int exitCode = CsvDatasetGenerator.run(
                    arguments,
                    new PrintStream(standardOutput, true, StandardCharsets.UTF_8),
                    new PrintStream(standardError, true, StandardCharsets.UTF_8));

            assertEquals(2, exitCode);
            assertTrue(standardError.toString(StandardCharsets.UTF_8).contains("Usage:"));
            assertFalse(standardOutput.size() > 0);
        }
        assertFalse(Files.exists(output));
    }
}
