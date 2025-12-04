package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.like.LikeInfo;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;

public record ProductDetailInfo(Long id, String name, BigDecimal price, long stock
    , BrandInfo brandInfo, LikeInfo likeInfo) {
  public static ProductDetailInfo from(ProductStock model, LikeInfo likeInfo) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "상품정보를 찾을수 없습니다.");
    return new ProductDetailInfo(
        model.product().getId(),
        model.product().getName(),
        model.product().getPrice().getAmount(),
        model.stock().getAvailable(),
        BrandInfo.from(model.product().getBrand()),
        likeInfo
    );
  }
}
