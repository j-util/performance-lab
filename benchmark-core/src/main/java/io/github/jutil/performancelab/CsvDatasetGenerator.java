package io.github.jutil.performancelab;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** Generates deterministic CSV input files independently of benchmark execution. */
public final class CsvDatasetGenerator {

    private static final long SEED = 0x4A5554494C4C4142L;
    private static final long BASE_TIMESTAMP_MILLIS = 1_704_067_200_000L;
    private static final int TIMESTAMP_RANGE_SECONDS = 366 * 24 * 60 * 60;
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("target", "benchmark-data");

    /* Repeated entries intentionally provide stable, non-uniform distributions. */
    private static final String[] REGIONS = {
        "NORTH_AMERICA", "NORTH_AMERICA", "NORTH_AMERICA",
        "EUROPE", "EUROPE", "EUROPE",
        "ASIA_PACIFIC", "ASIA_PACIFIC",
        "LATIN_AMERICA", "MIDDLE_EAST_AFRICA"
    };
    private static final String[] STATUSES = {
        "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED",
        "PENDING", "PENDING", "CANCELLED", "REFUNDED"
    };
    private static final String[] HEADER = {
        "id", "customerId", "productId", "quantity", "priceCents", "timestamp", "region", "status"
    };
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(HEADER)
            .setRecordSeparator('\n')
            .get();

    private CsvDatasetGenerator() {
    }

    /**
     * Generates a deterministic dataset, writing each row before generating the next one.
     *
     * @param rowCount number of data rows to generate; must be positive
     * @param outputPath destination CSV path
     * @return the supplied output path
     * @throws IllegalArgumentException if {@code rowCount} is not positive
     * @throws NullPointerException if {@code outputPath} is {@code null}
     * @throws IOException if the destination cannot be created or written
     */
    public static Path generate(long rowCount, Path outputPath) throws IOException {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Row count must be a positive integer: " + rowCount);
        }
        Objects.requireNonNull(outputPath, "outputPath");

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Random random = new Random(SEED);
        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {
            for (long index = 0; index < rowCount; index++) {
                print(printer, nextRow(index + 1, random));
            }
        }
        return outputPath;
    }

    /**
     * Command-line entry point. Usage: {@code CsvDatasetGenerator <row-count> [output-path]}.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length < 1 || args.length > 2) {
            return invalidArguments(err, "Expected a row count and, optionally, an output path.");
        }

        long rowCount;
        try {
            rowCount = Long.parseLong(args[0]);
        } catch (NumberFormatException exception) {
            return invalidArguments(err, "Row count must be a positive integer: " + args[0]);
        }
        if (rowCount <= 0) {
            return invalidArguments(err, "Row count must be a positive integer: " + args[0]);
        }

        Path outputPath;
        try {
            if (args.length == 2) {
                if (args[1].isBlank()) {
                    return invalidArguments(err, "Output path must not be blank.");
                }
                outputPath = Path.of(args[1]);
            } else {
                outputPath = DEFAULT_OUTPUT_DIRECTORY.resolve("benchmark-rows-" + rowCount + ".csv");
            }
        } catch (InvalidPathException exception) {
            return invalidArguments(err, "Invalid output path: " + exception.getInput());
        }

        try {
            generate(rowCount, outputPath);
        } catch (IOException exception) {
            err.println("Failed to generate dataset at " + outputPath.toAbsolutePath().normalize()
                    + ": " + exception.getMessage());
            return 1;
        }

        out.println("Generated " + rowCount + " rows at " + outputPath.toAbsolutePath().normalize());
        return 0;
    }

    private static BenchmarkRow nextRow(long id, Random random) {
        long customerId = 1L + random.nextInt(100_000);
        int productId = 1 + random.nextInt(10_000);
        int quantity = 1 + random.nextInt(20);
        long priceCents = 199L + random.nextInt(49_802);
        long timestamp = BASE_TIMESTAMP_MILLIS + random.nextInt(TIMESTAMP_RANGE_SECONDS) * 1_000L;
        String region = REGIONS[random.nextInt(REGIONS.length)];
        String status = STATUSES[random.nextInt(STATUSES.length)];
        return new BenchmarkRow(id, customerId, productId, quantity, priceCents, timestamp, region, status);
    }

    private static void print(CSVPrinter printer, BenchmarkRow row) throws IOException {
        printer.printRecord(
                row.id(),
                row.customerId(),
                row.productId(),
                row.quantity(),
                row.priceCents(),
                row.timestamp(),
                row.region(),
                row.status());
    }

    private static int invalidArguments(PrintStream err, String message) {
        err.println("Error: " + message);
        err.println("Usage: CsvDatasetGenerator <row-count> [output-path]");
        return 2;
    }
}
