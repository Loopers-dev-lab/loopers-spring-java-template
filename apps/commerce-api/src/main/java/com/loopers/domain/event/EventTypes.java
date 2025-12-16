package com.loopers.domain.event;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
public final class EventTypes {
    private EventTypes() {}

    public static final String PAYMENT_SUCCESS = "PAYMENT_SUCCESS";
    public static final String PRODUCT_VIEW = "PRODUCT_VIEW";
    public static final String LIKE_ACTION = "LIKE_ACTION";
    public static final String ORDER_CREATE = "ORDER_CREATE";
}
