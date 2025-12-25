package com.loopers.kafka;

public final class AggregateTypes {

    public static final String PRODUCT_LIKE = "PRODUCT_LIKE";
    public static final String ORDER = "ORDER";
    public static final String COUPON = "COUPON";
    public static final String ACTIVITY = "ACTIVITY";
    public static final String PRODUCT_VIEW = "PRODUCT_VIEW";

    private AggregateTypes() {
        throw new AssertionError("Cannot instantiate utility class");
    }
}
