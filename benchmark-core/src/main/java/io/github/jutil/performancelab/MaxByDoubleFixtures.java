package io.github.jutil.performancelab;

/** Deterministic input data for the maximum-by-double workload. */
final class MaxByDoubleFixtures {

    private static final int PRODUCT_COUNT = 128;
    private static final int WINNING_QUANTITY = 1_000_000;
    private static final int REGULAR_QUANTITY_RANGE = 1_000;
    private static final Product[] PRODUCTS = createProducts();

    private MaxByDoubleFixtures() {
    }

    static Position positionAt(int rowIndex, int rowCount) {
        validateRowCount(rowCount);
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IllegalArgumentException(
                    "Row index must be between 0 and " + (rowCount - 1) + ": " + rowIndex);
        }

        Product product = PRODUCTS[(int) (((long) rowIndex * 31L + 7L) % PRODUCT_COUNT)];
        int quantity = rowIndex == winningIndex(rowCount)
                ? WINNING_QUANTITY
                : 1 + (int) (((long) rowIndex * 17L + 11L) % REGULAR_QUANTITY_RANGE);
        Position position = new Position(rowIndex, quantity, product);
        if (!Double.isFinite(position.marketValue())) {
            throw new IllegalStateException(
                    "Generated a non-finite market value at row " + rowIndex);
        }
        return position;
    }

    static Position[] newPositions(int rowCount) {
        validateRowCount(rowCount);
        Position[] positions = new Position[rowCount];
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            positions[rowIndex] = positionAt(rowIndex, rowCount);
        }
        return positions;
    }

    static int winningIndex(int rowCount) {
        validateRowCount(rowCount);
        return rowCount == 1 ? 0 : rowCount / 2;
    }

    static Position expectedWinner(int rowCount) {
        return positionAt(winningIndex(rowCount), rowCount);
    }

    static void validateRowCount(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("Row count must be positive: " + rowCount);
        }
    }

    private static Product[] createProducts() {
        Product[] products = new Product[PRODUCT_COUNT];
        for (int productIndex = 0; productIndex < products.length; productIndex++) {
            double price = 10.0d + ((productIndex * 379L) % 1_900L) / 10.0d;
            products[productIndex] = new Product(productIndex, price);
        }
        return products;
    }
}
