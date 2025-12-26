package com.loopers.application.product;

import com.loopers.domain.product.Product;

import java.io.Serializable;

public record ProductDetailInfo(
        Long productId,
        String productName,
        Long price,
        Integer stock,
        Long brandId,
        String brandName,
        Long likeCount,
        Long rank
) implements Serializable {
    public static ProductDetailInfo of(Product product, Long likeCount) {
        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                product.getStockValue(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                likeCount,
                null
        );
    }

    public static ProductDetailInfo of(Product product, Long likeCount, Long rank) {
        return new ProductDetailInfo(
                product.getId(),
                product.getName(),
                product.getPriceValue(),
                product.getStockValue(),
                product.getBrand().getId(),
                product.getBrand().getName(),
                likeCount,
                rank
        );
    }

    public ProductDetailInfo withRank(Long rank) {
        return new ProductDetailInfo(
                this.productId,
                this.productName,
                this.price,
                this.stock,
                this.brandId,
                this.brandName,
                this.likeCount,
                rank
        );
    }
}
