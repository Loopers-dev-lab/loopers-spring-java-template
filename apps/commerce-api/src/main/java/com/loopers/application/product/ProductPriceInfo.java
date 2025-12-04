package com.loopers.application.product;

import java.math.BigDecimal;

/**
 * 상품 가격 정보 DTO
 */
public record ProductPriceInfo(
        BigDecimal originPrice,
        BigDecimal discountPrice
) {
}
