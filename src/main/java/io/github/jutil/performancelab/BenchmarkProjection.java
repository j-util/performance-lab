package io.github.jutil.performancelab;

import io.github.jutil.columnarprojection.ProjectionSchema;

/** Full-row projection shared by object and columnar benchmark cases. */
@ProjectionSchema
public interface BenchmarkProjection {

    long id();

    long customerId();

    int productId();

    int quantity();

    long priceCents();

    long timestamp();

    String region();

    String status();
}
