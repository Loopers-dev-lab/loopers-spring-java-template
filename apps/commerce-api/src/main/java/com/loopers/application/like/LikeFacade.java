package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
@Component
public class LikeFacade {

  private final ProductService productService;
  private final LikeService likeService;
  private final TransactionTemplate transactionTemplate;

  private static final int RETRY_COUNT = 30;

  public LikeInfo like(long userId, long productId) {
    Optional<Like> existingLike = likeService.findLike(userId, productId);


    if (existingLike.isPresent()) {
      Product product = productService.getProduct(productId);
      return LikeInfo.from(existingLike.get(), product.getLikeCount());
    }

    for (int i = 0; i < RETRY_COUNT; i++) {
      try {

        Like newLike = likeService.save(userId, productId);
        int updatedLikeCount = productService.increaseLikeCount(productId);

        return LikeInfo.from(newLike, updatedLikeCount);
      } catch (ObjectOptimisticLockingFailureException e) {
        if (i == RETRY_COUNT - 1) {
          throw e;
        }
        sleep(50);
      }
    }

    throw new IllegalStateException("좋아요 처리 재시도 횟수를 초과했습니다.");
  }

  public int unLike(long userId, long productId) {
    for (int i = 0; i < RETRY_COUNT; i++) {
      try {

        return transactionTemplate.execute(status -> {

          likeService.unLike(userId, productId);

          return productService.decreaseLikeCount(productId);

        });

      } catch (ObjectOptimisticLockingFailureException e) {

        if (i == RETRY_COUNT - 1) {
          throw e;
        }
        sleep(50);
      }
    }
    throw new IllegalStateException("싫어요 처리 재시도 횟수를 초과했습니다.");
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
