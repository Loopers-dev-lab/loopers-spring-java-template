package com.loopers.application.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.ProductLikeService;
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
    Product product = productService.getById(productId);

    boolean alreadyLiked = productLikeService.isLiked(userId, productId);
    if (alreadyLiked) {
      return;
    }

    productLikeService.createLike(userId, productId);
    product.increaseLikeCount();
  }

  public void unlikeProduct(Long userId, Long productId) {
    Product product = productService.getById(productId);

    boolean liked = productLikeService.isLiked(userId, productId);
    if (!liked) {
      return;
    }

    productLikeService.deleteLike(userId, productId);
    product.decreaseLikeCount();
  }
}
