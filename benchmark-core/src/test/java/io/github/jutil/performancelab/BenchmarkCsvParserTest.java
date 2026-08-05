package io.github.jutil.performancelab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BenchmarkCsvParserTest {

    @Test
    void mapsEveryCsvFieldAndLeavesCallerOwnedInputOpen() throws Exception {
        String csv = "id,customerId,productId,quantity,priceCents,timestamp,region,status\n"
                + "42,501,73,4,1299,1704067200123,\"NORTH, AMERICA\",COMPLETED\n";
        CloseTrackingInputStream input =
                new CloseTrackingInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<BenchmarkRow> rows = new ArrayList<>();

        BenchmarkCsvParser.parseRows(input, rows::add);

        assertEquals(List.of(new BenchmarkRow(
                42L, 501L, 73, 4, 1299L, 1_704_067_200_123L,
                "NORTH, AMERICA", "COMPLETED")), rows);
        assertFalse(input.closed);
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
