package com.loopers.domain.product;

import java.math.BigDecimal;

public class ProductOptionFixture {

    public static final Long DEFAULT_PRODUCT_ID = 1L;
    public static final String DEFAULT_OPTION_NAME = "색상";
    public static final String DEFAULT_OPTION_VALUE = "빨간색";
    public static final BigDecimal DEFAULT_ADDITIONAL_PRICE = new BigDecimal(5000);

    public static ProductOptionModel createProductOption() {
        return ProductOptionModel.create(
                DEFAULT_PRODUCT_ID,
                DEFAULT_OPTION_NAME,
                DEFAULT_OPTION_VALUE,
                DEFAULT_ADDITIONAL_PRICE
        );
    }

    public static ProductOptionModel createProductOption(Long productId, String name, String value, BigDecimal additionalPrice) {
        return ProductOptionModel.create(productId, name, value, additionalPrice);
    }

    public static ProductOptionModel createWithProductId(Long productId) {
        return createProductOption(productId, DEFAULT_OPTION_NAME, DEFAULT_OPTION_VALUE, DEFAULT_ADDITIONAL_PRICE);
    }

    public static ProductOptionModel createWithOptionName(String optionName) {
        return createProductOption(DEFAULT_PRODUCT_ID, optionName, DEFAULT_OPTION_VALUE, DEFAULT_ADDITIONAL_PRICE);
    }

    public static ProductOptionModel createWithOptionValue(String optionValue) {
        return createProductOption(DEFAULT_PRODUCT_ID, DEFAULT_OPTION_NAME, optionValue, DEFAULT_ADDITIONAL_PRICE);
    }

    public static ProductOptionModel createWithAdditionalPrice(BigDecimal additionalPrice) {
        return createProductOption(DEFAULT_PRODUCT_ID, DEFAULT_OPTION_NAME, DEFAULT_OPTION_VALUE, additionalPrice);
    }

    public static ProductOptionModel createFreeOption() {
        return createWithAdditionalPrice(BigDecimal.ZERO);
    }

    public static ProductOptionModel createDiscountOption() {
        return createProductOption(DEFAULT_PRODUCT_ID, "할인", "10% 할인", new BigDecimal(-1000));
    }

    public static ProductOptionModel createSizeOption(String size, BigDecimal additionalPrice) {
        return createProductOption(DEFAULT_PRODUCT_ID, "사이즈", size, additionalPrice);
    }

    public static ProductOptionModel createColorOption(String color, BigDecimal additionalPrice) {
        return createProductOption(DEFAULT_PRODUCT_ID, "색상", color, additionalPrice);
    }
}
