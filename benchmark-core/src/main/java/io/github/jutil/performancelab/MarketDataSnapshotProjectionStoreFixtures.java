package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionStore;
import io.github.jutil.columnarprojection.ProjectionStores;

/** Construction and validation of sealed Columnar Projection Store market-data fixtures. */
final class MarketDataSnapshotProjectionStoreFixtures {

    private MarketDataSnapshotProjectionStoreFixtures() {
    }

    static ProjectionStore<MarketDataSnapshotProjection> newStore(int rowCount) {
        MarketDataSnapshotFixtures.validateRowCount(rowCount);
        ProjectionStore<MarketDataSnapshotProjection> store =
                ProjectionStores.create(MarketDataSnapshotProjection.class, rowCount);
        addSnapshots(rowCount, store::add);
        validateSize("Columnar Projection Store", rowCount, store.size());
        store.seal();
        return store;
    }

    private static void addSnapshots(
            int rowCount, java.util.function.Consumer<MarketDataSnapshot> destination) {
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            destination.accept(MarketDataSnapshotFixtures.snapshotAt(rowIndex));
        }
    }

    private static void validateSize(String representation, int expected, long actual) {
        if (actual != expected) {
            throw new IllegalStateException(
                    representation + " contains " + actual + " rows; expected " + expected);
        }
    }
}
