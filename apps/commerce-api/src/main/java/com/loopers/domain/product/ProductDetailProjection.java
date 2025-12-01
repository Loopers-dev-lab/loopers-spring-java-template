package com.loopers.domain.product;

import com.loopers.domain.common.vo.Money;

public interface ProductDetailProjection {
    Long getProductId();
    String getProductName();
    Long getBrandId();
    String getBrandName();
    Money getPrice();
    int getLikeCount();
}
