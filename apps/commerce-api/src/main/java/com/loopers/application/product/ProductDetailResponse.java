package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.detail.ProductDetail;

public record ProductDetailResponse(
    Long id,
    String name,
    Long price,
    String description,
    Long stock,
    BrandSummary brand,
    Long likeCount,
    boolean isLiked
) {

  public static ProductDetailResponse of(Product product, BrandSummary brandSummary, boolean isLiked) {
    return new ProductDetailResponse(
        product.getId(),
        product.getName(),
        product.getPriceValue(),
        product.getDescription(),
        product.getStockValue(),
        brandSummary,
        product.getLikeCount(),
        isLiked
    );
  }

  public static ProductDetailResponse from(ProductDetail productDetail) {
    return new ProductDetailResponse(
        productDetail.getProductId(),
        productDetail.getProductName(),
        productDetail.getPrice(),
        productDetail.getDescription(),
        productDetail.getStockValue(),
        new BrandSummary(productDetail.getBrandId(), productDetail.getBrandName()),
        productDetail.getLikeCount(),
        productDetail.isLiked()
    );
  }
}
