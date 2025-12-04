package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
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
        BrandInfo brand
) {
    public static ProductDetailInfo from(Product product) {
        return new ProductDetailInfo(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice().getAmount(),
                product.getStockQuantity(),
                product.getLikeCount(),
                product.getBrand() != null ? BrandInfo.from(product.getBrand()) : null
        );
    }

    public static List<ProductDetailInfo> from(List<Product> products) {
        return products.stream().map(ProductDetailInfo::from).toList();
    }
}
