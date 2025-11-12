package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.detail.ProductDetail;

public record ProductListResponse(
    Long id,
    String name,
    Long price,
    BrandSummary brand,
    Long likeCount,
    boolean isLiked
) {

  public static ProductListResponse of(Product product, BrandSummary brandSummary, boolean isLiked) {
    return new ProductListResponse(
        product.getId(),
        product.getName(),
        product.getPriceValue(),
        brandSummary,
        product.getLikeCount(),
        isLiked
    );
  }

  public static ProductListResponse from(ProductDetail productDetail) {
    return new ProductListResponse(
        productDetail.getProductId(),
        productDetail.getProductName(),
        productDetail.getPrice(),
        new BrandSummary(productDetail.getBrandId(), productDetail.getBrandName()),
        productDetail.getLikeCount(),
        productDetail.isLiked()
    );
  }
}
