package com.loopers.application.like;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {

  private final ProductLikeService productLikeService;
  private final ProductService productService;

  @Transactional
  public void likeProduct(Long userId, Long productId) {
    productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    try {
      productLikeService.createLike(userId, productId);
      productService.increaseLikeCount(productId);
    } catch (DataIntegrityViolationException e) {

    }
  }

  @Transactional
  public void unlikeProduct(Long userId, Long productId) {
    productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    int deleted = productLikeService.deleteLike(userId, productId);

    if (deleted > 0) {
      productService.decreaseLikeCount(productId);
    }
  }
}
