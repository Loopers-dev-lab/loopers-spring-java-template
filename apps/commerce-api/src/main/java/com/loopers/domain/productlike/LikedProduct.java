package com.loopers.domain.productlike;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import java.time.LocalDateTime;
import java.util.Objects;

public record LikedProduct(
    Long productId,
    String productName,
    Long price,
    Long likeCount,
    Long brandId,
    String brandName,
    LocalDateTime likedAt
) {

  public static LikedProduct of(ProductLike productLike, Product product, Brand brand) {
    Objects.requireNonNull(productLike, "ProductLike는 null일 수 없습니다.");
    Objects.requireNonNull(product, "Product는 null일 수 없습니다.");
    Objects.requireNonNull(brand, "Brand는 null일 수 없습니다.");

    return new LikedProduct(
        product.getId(),
        product.getName(),
        product.getPriceValue(),
        product.getLikeCount(),
        brand.getId(),
        brand.getName(),
        productLike.getLikedAt()
    );
  }
}
