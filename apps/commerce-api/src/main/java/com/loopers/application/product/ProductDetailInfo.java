package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.like.LikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;

public record ProductDetailInfo(Long id, String name, BigDecimal price, long stock
    , BrandInfo brandInfo, LikeInfo likeInfo) {
  public static ProductDetailInfo from(Product model, boolean isLiked) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "상품정보를 찾을수 없습니다.");
    return new ProductDetailInfo(
        model.getId(),
        model.getName(),
        model.getPrice().getAmount(),
        model.getStock(),
        BrandInfo.from(model.getBrand()),
        LikeInfo.from(model.getLikeCount(), isLiked)
    );
  }
}
