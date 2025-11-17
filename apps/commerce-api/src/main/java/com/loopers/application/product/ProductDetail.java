package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public record ProductDetail(
    Long productId,
    String productName,
    Long price,
    String description,
    Long stock,
    Long brandId,
    String brandName,
    Long likeCount,
    boolean liked
) {

  public static ProductDetail of(Product product, Brand brand, boolean liked) {
    if (product == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_DETAIL_PRODUCT_EMPTY, "상품 정보는 필수입니다.");
    }
    if (brand == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_DETAIL_BRAND_EMPTY, "브랜드 정보는 필수입니다.");
    }

    return new ProductDetail(
        product.getId(),
        product.getName(),
        product.getPriceValue(),
        product.getDescription(),
        product.getStockValue(),
        brand.getId(),
        brand.getName(),
        product.getLikeCount(),
        liked
    );
  }
}
