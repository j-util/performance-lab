package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class OneBrcStyleCsvParserTest {

    @Test
    void emptyAndMalformedRecordsFailAtBothParserLifecycleBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> OneBrcStyleCsvParser.parseLine(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> OneBrcStyleCsvParser.parseLine("Yerevan;1.0;unexpected"));

        assertBulkParseFails("\n");
        assertBulkParseFails("Yerevan;1.0;unexpected\n");
    }

    private static void assertBulkParseFails(String content) {
        assertThrows(IllegalArgumentException.class, () -> new OneBrcStyleCsvParser().parse(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                ignored -> {
                }));
    }
}
