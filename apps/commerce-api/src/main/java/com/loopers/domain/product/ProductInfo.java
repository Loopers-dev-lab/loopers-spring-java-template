package com.loopers.domain.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductInfo {
    private final Long id;
    private final String name;
    private final int stockQuantity;
    private final BigDecimal priceAmount;
    private final String brandName;
    private final long likeCount;
}

