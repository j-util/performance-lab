package io.github.jutil.performancelab;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;

import org.apache.commons.csv.CSVPrinter;

/** Generates deterministic, headerless station-temperature input outside JMH execution. */
public final class OneBrcStyleDatasetGenerator {

    public static final long DEFAULT_ROW_COUNT = 10_000_000L;

    private static final long SEED = 0x314252432D535459L;
    private static final Path DEFAULT_OUTPUT_DIRECTORY = Path.of("target", "benchmark-data");
    private static final String[] STATIONS = {
        "Yerevan", "Berlin", "London", "Paris", "Madrid", "Rome", "Vienna", "Prague",
        "Warsaw", "Helsinki", "Oslo", "Stockholm", "Reykjavík", "Zürich", "Tbilisi",
        "Tokyo", "Seoul", "Singapore", "Bangkok", "Mumbai", "Cairo", "Nairobi",
        "Cape Town", "São Paulo", "Buenos Aires", "Lima", "Mexico City", "Toronto",
        "New York", "Chicago", "San Francisco", "Sydney"
    };
    private static final int[] BASE_TEMPERATURE_TENTHS = {
        126, 104, 118, 123, 151, 158, 112, 90,
        90, 60, 70, 70, 50, 90, 130,
        159, 129, 278, 284, 270, 224, 201,
        167, 216, 183, 194, 176, 90,
        131, 104, 143, 180
    };

    private OneBrcStyleDatasetGenerator() {
    }

    /** Generates exactly {@code rowCount} UTF-8 records, replacing the destination if needed. */
    public static Path generate(long rowCount, Path outputPath) throws IOException {
        if (rowCount <= 0L) {
            throw new IllegalArgumentException("Row count must be a positive integer: " + rowCount);
        }
        Objects.requireNonNull(outputPath, "outputPath");

        Path parent = outputPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Random random = new Random(SEED);
        try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, OneBrcStyleCsvParser.csvFormat())) {
            for (long index = 0; index < rowCount; index++) {
                int stationIndex = random.nextInt(STATIONS.length);
                int temperatureTenths = BASE_TEMPERATURE_TENTHS[stationIndex]
                        + random.nextInt(401) - 200;
                printer.printRecord(
                        STATIONS[stationIndex],
                        formatTemperature(temperatureTenths));
            }
        }
        return outputPath;
    }

    /** Returns the conventional generated-data path for a row count. */
    public static Path defaultOutputPath(long rowCount) {
        return DEFAULT_OUTPUT_DIRECTORY.resolve(
                "1brc-style-measurements-" + rowCount + ".csv");
    }

    /** Command-line entry point. Usage: {@code OneBrcStyleDatasetGenerator [row-count] [output-path]}. */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length > 2) {
            return invalidArguments(err, "Expected an optional row count and output path.");
        }

        long rowCount = DEFAULT_ROW_COUNT;
        if (args.length >= 1) {
            try {
                rowCount = Long.parseLong(args[0]);
            } catch (NumberFormatException exception) {
                return invalidArguments(err, "Row count must be a positive integer: " + args[0]);
            }
            if (rowCount <= 0L) {
                return invalidArguments(err, "Row count must be a positive integer: " + args[0]);
            }
        }

        Path outputPath;
        try {
            if (args.length == 2) {
                if (args[1].isBlank()) {
                    return invalidArguments(err, "Output path must not be blank.");
                }
                outputPath = Path.of(args[1]);
            } else {
                outputPath = defaultOutputPath(rowCount);
            }
        } catch (InvalidPathException exception) {
            return invalidArguments(err, "Invalid output path: " + exception.getInput());
        }

        try {
            if (hasMatchingRowCount(outputPath, rowCount)) {
                out.println("Reusing " + rowCount + " rows at "
                        + outputPath.toAbsolutePath().normalize());
                return 0;
            }
            generate(rowCount, outputPath);
        } catch (IOException exception) {
            err.println("Failed to generate dataset at " + outputPath.toAbsolutePath().normalize()
                    + ": " + exception.getMessage());
            return 1;
        }

        out.println("Generated " + rowCount + " rows at "
                + outputPath.toAbsolutePath().normalize());
        return 0;
    }

    private static boolean hasMatchingRowCount(Path outputPath, long rowCount) throws IOException {
        if (!Files.isRegularFile(outputPath)) {
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(outputPath, StandardCharsets.UTF_8)) {
            return reader.lines().count() == rowCount;
        }
    }

    private static String formatTemperature(int temperatureTenths) {
        int absolute = Math.abs(temperatureTenths);
        String sign = temperatureTenths < 0 ? "-" : "";
        return sign + absolute / 10 + "." + absolute % 10;
    }

    private static int invalidArguments(PrintStream err, String message) {
        err.println("Error: " + message);
        err.println("Usage: OneBrcStyleDatasetGenerator [row-count] [output-path]");
        return 2;
    }
}
