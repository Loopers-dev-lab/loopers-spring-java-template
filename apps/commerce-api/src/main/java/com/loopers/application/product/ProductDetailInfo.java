package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.domain.product.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailInfo(
        Long id,
        String productCode,
        String productName,
        BigDecimal price,
        int stock,
        Long likeCount,
        BrandInfo brand,
        RankingInfo.ProductRankings rankings
) {
    public static ProductDetailInfo from(Product product) {
        return new ProductDetailInfo(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getStockQuantity(),
                product.getLikeCount(),
                product.getBrand() != null ? BrandInfo.from(product.getBrand()) : null,
                null
        );
    }

    // Product + Rankings를 받는 경우 사용
    public static ProductDetailInfo of(Product product, RankingInfo.ProductRankings rankings) {
        return new ProductDetailInfo(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getStockQuantity(),
                product.getLikeCount(),
                product.getBrand() != null ? BrandInfo.from(product.getBrand()) : null,
                rankings
        );
    }

    public static List<ProductDetailInfo> from(List<Product> products) {
        return products.stream().map(ProductDetailInfo::from).toList();
    }
}
