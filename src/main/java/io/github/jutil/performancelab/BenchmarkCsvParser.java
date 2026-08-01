package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import io.github.jutil.inputstreamprocessor.core.InputParser;

/** Incremental Commons CSV parser for benchmark rows. */
final class BenchmarkCsvParser implements InputParser<BenchmarkRow> {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();

    @Override
    public void parse(InputStream input, Consumer<? super BenchmarkRow> emitter) throws IOException {
        parseRows(input, emitter);
    }

    /**
     * Parses and emits rows without closing the caller-owned input stream.
     */
    static void parseRows(InputStream input, Consumer<? super BenchmarkRow> emitter) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(emitter, "emitter");

        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        CSVParser parser = CSV_FORMAT.parse(reader);
        for (CSVRecord record : parser) {
            emitter.accept(toBenchmarkRow(record));
        }
    }

    private static BenchmarkRow toBenchmarkRow(CSVRecord record) {
        return new BenchmarkRow(
                Long.parseLong(record.get("id")),
                Long.parseLong(record.get("customerId")),
                Integer.parseInt(record.get("productId")),
                Integer.parseInt(record.get("quantity")),
                Long.parseLong(record.get("priceCents")),
                Long.parseLong(record.get("timestamp")),
                record.get("region"),
                record.get("status"));
    }
}
