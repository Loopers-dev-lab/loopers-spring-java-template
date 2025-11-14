package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductInfo(
        Long id
        , String name
        , String description
        , BigDecimal price
        , Long likeCount
        , ProductStatus status
        , Boolean isVisible
        , Boolean isSellable
        , BrandInfo brand
) {
    public static ProductInfo from(Product product) {
        return ProductInfo.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .likeCount(product.getLikeCount())
                .status(product.getStatus())
                .isVisible(product.getIsVisible())
                .isSellable(product.getIsSellable())
                .brand(product.getBrand() != null ? BrandInfo.from(product.getBrand()) : null)
                .build();
    }

    public static ProductInfo from(ProductService.ProductWithBrand productWithBrand) {
        Product product = productWithBrand.getProduct();
        return ProductInfo.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .likeCount(product.getLikeCount())
                .status(product.getStatus())
                .isVisible(product.getIsVisible())
                .isSellable(product.getIsSellable())
                .brand(BrandInfo.from(productWithBrand.getBrand()))
                .build();
    }
}

