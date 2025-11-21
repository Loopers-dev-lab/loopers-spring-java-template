package com.loopers.application.product;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.\
 * <p>
 * Price 정보를 담는 DTO
 */
public record ProductPriceInfo(
        BigDecimal originPrice,
        BigDecimal discountPrice
) {
}
