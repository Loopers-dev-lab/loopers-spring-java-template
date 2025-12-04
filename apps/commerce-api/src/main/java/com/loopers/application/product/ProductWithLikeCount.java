package com.loopers.application.product;

import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.math.BigDecimal;

public record ProductWithLikeCount(Long id, String name, BigDecimal price
    , long likeCount) {
  public static ProductWithLikeCount from(Product model, long likeCount) {
    if (model == null) throw new CoreException(ErrorType.NOT_FOUND, "상품정보를 찾을수 없습니다.");
    return new ProductWithLikeCount(
        model.getId(),
        model.getName(),
        model.getPrice().getAmount(),
        likeCount
    );
  }

  public static ProductWithLikeCount from(Long id, String name, BigDecimal price
      , long likeCount) {
    return new ProductWithLikeCount(
        id,
        name,
        price,
        likeCount
    );
  }
}
