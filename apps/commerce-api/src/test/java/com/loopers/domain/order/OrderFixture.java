package com.loopers.domain.order;

import com.loopers.domain.order.item.OrderItemModel;

import java.math.BigDecimal;

public class OrderFixture {

    public static final String ODER_NUMBER = "ORD-20250807213015999-1A2B3C4D";
    public static final Long ODER_USER_ID = 1L;
    public static final Long ODER_PRODUCT_ID = 1L;
    public static final Long ODER_OPTION_ID = 1L;
    public static final int ODER_QUANTITY = 2;
    public static final BigDecimal ODER_PRICE_PER_UNIT = new BigDecimal("10000");
    public static final String ODER_ORDER_STATUS = "PAYMENT_COMPLETED";
    public static final String ODER_PRODUCT_NAME = "Test Product";
    public static final String ODER_OPTION_NAME = "ODER Option";
    public static final String ODER_IMAGE_URL = "http://example.com/image.jpg";

    public static OrderModel createOrderModel() {
        OrderModel order = OrderModel.of(ODER_NUMBER,ODER_USER_ID,ODER_ORDER_STATUS,BigDecimal.ZERO);
        OrderItemModel.of(order.getId(),ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }

    public static OrderModel createOrderWithStatus(String status) {
        OrderModel order = OrderModel.of(ODER_NUMBER,ODER_USER_ID, status,BigDecimal.ZERO);
        order.addItem(ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }

    public static OrderModel createOrderWithUserId(Long userId) {
        OrderModel order = OrderModel.of(ODER_NUMBER, userId,ODER_ORDER_STATUS,BigDecimal.ZERO);

        order.addItem(ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }
    public static OrderModel createOrderWithOrderNumber(String orderNumber) {
        OrderModel order = OrderModel.of(orderNumber, ODER_USER_ID,ODER_ORDER_STATUS,BigDecimal.ZERO);
        order.addItem(ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }

    public static OrderModel createOrderWithItem(Long productId, Long optionId, int quantity, BigDecimal price, String name, String option, String imageUrl) {
        OrderModel order = OrderModel.of(ODER_NUMBER,ODER_USER_ID,ODER_ORDER_STATUS,BigDecimal.ZERO);
        order.addItem(productId, optionId, quantity, price, name, option, imageUrl);
        return order;
    }
    public static OrderModel createOrderWithOrderStatus(String status) {
        OrderModel order = OrderModel.of(ODER_NUMBER, ODER_USER_ID,status, BigDecimal.ZERO);
        order.addItem(ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }
    public static OrderModel createOrderWithOrderPrice(BigDecimal price) {
        OrderModel order = OrderModel.of(ODER_NUMBER, ODER_USER_ID,ODER_ORDER_STATUS, price);
        order.addItem(ODER_PRODUCT_ID,ODER_OPTION_ID,ODER_QUANTITY,ODER_PRICE_PER_UNIT,ODER_PRODUCT_NAME,ODER_OPTION_NAME,ODER_IMAGE_URL);
        return order;
    }
}

