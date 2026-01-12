package com.loopers.application.product;

import com.loopers.domain.product.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.product.ProductModel;

public record ProductInfo(Long id, String name, Brand brand, Money price, Long likeCount) {
    public static ProductInfo from(ProductModel model) {
        return new ProductInfo(
            model.getId(),
            model.getName(),
            model.getBrand(),
            model.getPrice(),
            model.getLikeCount()
        );
    }
}   
