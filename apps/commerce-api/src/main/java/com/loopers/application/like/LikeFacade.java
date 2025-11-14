package com.loopers.application.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class LikeFacade {

  private final ProductLikeService productLikeService;
  private final ProductService productService;

  public void likeProduct(Long userId, Long productId) {
    Product product = productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    boolean alreadyLiked = productLikeService.isLiked(userId, productId);
    if (alreadyLiked) {
      return;
    }

    productLikeService.createLike(userId, productId);
    product.increaseLikeCount();
  }

  public void unlikeProduct(Long userId, Long productId) {
    Product product = productService.getById(productId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

    boolean liked = productLikeService.isLiked(userId, productId);
    if (!liked) {
      return;
    }

    productLikeService.deleteLike(userId, productId);
    product.decreaseLikeCount();
  }
}
