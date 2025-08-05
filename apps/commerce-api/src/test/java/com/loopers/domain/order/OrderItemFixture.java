package com.loopers.domain.order;

import java.math.BigDecimal;

public class OrderItemFixture {

    public static final Long ORDER_ITEM_PRODUCT_ID = 1L;
    public static final Long ORDER_ITEM_OPTION_ID = 1L;
    public static final BigDecimal ORDER_ITEM_QUANTITY = new BigDecimal(2);
    public static final BigDecimal ORDER_ITEM_PRICE_PER_UNIT = new BigDecimal("10000");
    public static final String ORDER_ITEM_PRODUCT_NAME = "Test Product";
    public static final String ORDER_ITEM_OPTION_NAME = "Test Option";
    public static final String ORDER_ITEM_IMAGE_URL = "http://example.com/image.jpg";

    /**
     * 기본 OrderItemModel 생성
     */
    public static OrderItemModel createOrderItem(OrderModel orderModel) {
        return createOrderItem(
                orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }

    /**
     * 커스텀 값으로 OrderItemModel 생성
     */
    public static OrderItemModel createOrderItem(
            OrderModel orderModel,
            Long productId,
            Long optionId,
            BigDecimal quantity,
            BigDecimal pricePerUnit,
            String productName,
            String optionName,
            String imageUrl
    ) {
        OrderItemModel item = OrderItemModel.of(
                orderModel,
                productId,
                optionId,
                quantity,
                pricePerUnit,
                productName,
                optionName,
                imageUrl
        );
        item.setProductSnapshot(productName, optionName, imageUrl);
        return item;
    }

    public static OrderItemModel createWithProductName(OrderModel orderModel, String productName) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                productName,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }

    public static OrderItemModel createWithOptionName(OrderModel orderModel, String optionName) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                optionName,
                ORDER_ITEM_IMAGE_URL
        );
    }

    public static OrderItemModel createWithImageUrl(OrderModel orderModel, String imageUrl) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                imageUrl
        );
    }

    public static OrderItemModel createWithPrice(OrderModel orderModel, BigDecimal price) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                price,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }

    public static OrderItemModel createWithQuantity(OrderModel orderModel, BigDecimal quantity) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                ORDER_ITEM_OPTION_ID,
                quantity,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }

    public static OrderItemModel createWithProductId(OrderModel orderModel, Long productId) {
        return createOrderItem(
                orderModel,
                productId,
                ORDER_ITEM_OPTION_ID,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }

    public static OrderItemModel createWithOptionId(OrderModel orderModel, Long optionId) {
        return createOrderItem(orderModel,
                ORDER_ITEM_PRODUCT_ID,
                optionId,
                ORDER_ITEM_QUANTITY,
                ORDER_ITEM_PRICE_PER_UNIT,
                ORDER_ITEM_PRODUCT_NAME,
                ORDER_ITEM_OPTION_NAME,
                ORDER_ITEM_IMAGE_URL
        );
    }
}
