package com.loopers.domain.order.dto;

import java.math.BigDecimal;

/**
 * @author hyunjikoh
 * @since 2025. 11. 22.
 */
public record OrderItemData(
        Long productId,
        Long couponId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount
) {}
