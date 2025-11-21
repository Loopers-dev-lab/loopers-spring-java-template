package com.loopers.domain.like;

import lombok.Getter;

@Getter
public class ProductIdAndLikeCount {
  private final Long productId;
  private final Long likeCount;

  public ProductIdAndLikeCount(Long productId, Long likeCount) {
    this.productId = productId;
    this.likeCount = likeCount;
  }
}
