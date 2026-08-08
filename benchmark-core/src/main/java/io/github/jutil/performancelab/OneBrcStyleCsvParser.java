package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import io.github.jutil.inputstreamprocessor.core.InputParser;

/** Shared Apache Commons CSV mapping for the 1BRC-style processor workload. */
final class OneBrcStyleCsvParser implements InputParser<Storage> {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setIgnoreEmptyLines(false)
            .setIgnoreSurroundingSpaces(false)
            .setTrim(false)
            .setRecordSeparator('\n')
            .get();

    @Override
    public void parse(InputStream input, Consumer<? super Storage> emitter) throws IOException {
        Storage storage = new Storage();
        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        CSVParser parser = CSV_FORMAT.parse(reader);
        for (CSVRecord record : parser) {
            storage.store(toItem(record));
        }
        emitter.accept(storage);
    }

    static Item parseLine(String line) {
        try (CSVParser parser = CSV_FORMAT.parse(new StringReader(line))) {
            Iterator<CSVRecord> records = parser.iterator();
            if (!records.hasNext()) {
                throw new IllegalArgumentException("Expected one CSV record but found none");
            }
            Item item = toItem(records.next());
            if (records.hasNext()) {
                throw new IllegalArgumentException("Expected one CSV record but found more than one");
            }
            return item;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    static CSVFormat csvFormat() {
        return CSV_FORMAT;
    }

    private static Item toItem(CSVRecord record) {
        if (record.size() != 2) {
            throw new IllegalArgumentException(
                    "Expected two CSV fields but found " + record.size()
                            + " in record " + record.getRecordNumber());
        }
        return new Item(record.get(0), Double.parseDouble(record.get(1)));
    }
}
