package com.loopers.application.like;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.LikeSortType;
import com.loopers.domain.productlike.LikedProduct;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {

  private final ProductLikeService productLikeService;
  private final ProductService productService;

  @Transactional
  public void registerProductLike(Long userId, Long productId) {
    productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    boolean result = productLikeService.createLike(userId, productId);
    if (!result) {
      return;
    }
    productService.increaseLikeCount(productId);
  }

  @Transactional
  public void cancelProductLike(Long userId, Long productId) {
    productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    int deleted = productLikeService.deleteLike(userId, productId);

    if (deleted > 0) {
      productService.decreaseLikeCount(productId);
    }
  }

  @Transactional(readOnly = true)
  public Page<LikedProduct> retrieveLikedProducts(Long userId, LikeSortType sortType,
      Pageable pageable) {
    return productLikeService.findLikedProducts(userId, sortType, pageable);
  }
}
