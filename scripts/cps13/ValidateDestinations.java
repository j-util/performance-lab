import io.github.jutil.performancelab.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

/** Untimed, exhaustive validation of the exact release-performance matrix. */
public class ValidateDestinations {
    public static void main(String[] args) throws Exception {
        int cases = 0;
        for (int rowCount : new int[] {100000, 10000000}) {
            var source = HardwoodDestinationMaterializationCases.createSourceArrays(rowCount);
            try (var executor = Executors.newFixedThreadPool(8)) {
                for (int batchSize : new int[] {8192, 1000000}) {
                    for (String path : new String[] {"columnarSequentialRangedBatches",
                            "columnarSingleThreadedAppender",
                            "columnarFixedPoolPerBatchBarrierAppender",
                            "columnarFixedPoolPipelinedAppender", "arrayListRows"}) {
                        validate(source, batchSize, path, executor);
                        if (executor.submit(() -> 42).get() != 42) {
                            throw new AssertionError("caller-owned executor");
                        }
                        cases++;
                        System.out.printf("PASS rows=%d batch=%d path=%s size/capacity/order/eight-fields/sealing-as-applicable%n",
                                rowCount, batchSize, path);
                        System.gc(); // Validator only; no timing is collected here.
                    }
                }
            }
        }
        if (cases != 20) throw new AssertionError("matrix incomplete");
        System.out.println("PASS all 20 cases; 101000000 rows; 808000000 field comparisons");
    }

    private static void validate(HardwoodDestinationMaterializationCases.SourceArrays source,
            int batchSize, String path, java.util.concurrent.Executor executor) throws Exception {
        Object result = switch (path) {
            case "columnarSequentialRangedBatches" ->
                    HardwoodDestinationMaterializationCases.sequentialRangedBatches(source, batchSize);
            case "columnarSingleThreadedAppender" ->
                    HardwoodDestinationMaterializationCases.singleThreadedColumnAppender(source, batchSize);
            case "columnarFixedPoolPerBatchBarrierAppender" ->
                    HardwoodDestinationMaterializationCases.executorPerBatchBarrierColumnAppender(source, batchSize, executor);
            case "columnarFixedPoolPipelinedAppender" ->
                    HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(source, batchSize, executor);
            case "arrayListRows" -> HardwoodDestinationMaterializationCases.arrayListRows(source, batchSize);
            default -> throw new AssertionError(path);
        };
        var store = result instanceof HardwoodMarketDataProjectionStore s ? s : null;
        var rows = store == null ? (List<?>) result : null;
        int n = source.rowCount();
        if ((store == null ? rows.size() : store.size()) != n) throw new AssertionError("size");
        if (store != null) {
            if ((int) field(store, "capacity") != n || !(boolean) field(store, "sealed")) {
                throw new AssertionError("capacity/sealing");
            }
            store.seal(); // Idempotent and already sealed before this check.
            try {
                store.add(store.viewAt(0));
                throw new AssertionError("accepted a write after seal");
            } catch (IllegalStateException expected) { }
        } else if (((Object[]) field(rows, "elementData")).length != n) {
            throw new AssertionError("ArrayList capacity");
        }
        for (int i = 0; i < n; i++) {
            HardwoodMarketDataProjection row = store == null
                    ? (HardwoodMarketDataProjection) rows.get(i) : store.viewAt(i);
            if (row.timestamp() != source.timestamps()[i]
                    || row.sequenceNumber() != source.sequenceNumbers()[i]
                    || !Objects.equals(row.symbol(), source.symbols()[i])
                    || !Objects.equals(row.venue(), source.venues()[i])
                    || !Objects.equals(row.side(), source.sides()[i])
                    || Double.compare(row.bidPrice(), source.bidPrices()[i]) != 0
                    || Double.compare(row.askPrice(), source.askPrices()[i]) != 0
                    || Double.compare(row.lastTradePrice(), source.lastTradePrices()[i]) != 0) {
                throw new AssertionError(path + " row " + i);
            }
        }
    }

    private static Object field(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(value);
    }
}
