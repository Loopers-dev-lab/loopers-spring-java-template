package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;

import java.math.BigDecimal;

public record ProductInfo(
        Long id,
        String productCode,
        String productName,
        BigDecimal price,
        Long likeCount,
        BrandInfo brand
) {
}
