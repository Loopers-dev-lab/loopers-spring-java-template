package com.loopers.domain.product.detail;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

/**
 * Product와 Brand, ProductLike 정보를 조합한 도메인 객체
 */
public class ProductDetail {

  private final Product product;
  private final Brand brand;
  @Getter
  private final boolean isLiked;

  private ProductDetail(Product product, Brand brand, boolean isLiked) {
    validateProduct(product);
    validateBrand(brand);

    this.product = product;
    this.brand = brand;
    this.isLiked = isLiked;
  }

  public static ProductDetail of(Product product, Brand brand, boolean isLiked) {
    return new ProductDetail(product, brand, isLiked);
  }

  private void validateProduct(Product product) {
    if (product == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_DETAIL_PRODUCT_EMPTY, "상품 정보는 필수입니다.");
    }
  }

  private void validateBrand(Brand brand) {
    if (brand == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_DETAIL_BRAND_EMPTY, "브랜드 정보는 필수입니다.");
    }
  }

  public Long getProductId() {
    return product.getId();
  }

  public String getProductName() {
    return product.getName();
  }

  public Long getPrice() {
    return product.getPriceValue();
  }

  public String getDescription() {
    return product.getDescription();
  }

  public Long getStockValue() {
    return product.getStockValue();
  }

  public String getBrandName() {
    return brand.getName();
  }

  public Long getBrandId() {
    return brand.getId();
  }

  public Long getLikeCount() {
    return product.getLikeCount();
  }
}
