package com.loopers.application.like;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnlikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

  private final ProductService productService;

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleProductLiked(ProductLikedEvent event) {
    log.info("[Event:ProductLiked] userId={}, productId={}", event.userId(), event.productId());
    productService.increaseLikeCount(event.productId());
  }

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handleProductUnliked(ProductUnlikedEvent event) {
    log.info("[Event:ProductUnliked] userId={}, productId={}", event.userId(), event.productId());
    productService.decreaseLikeCount(event.productId());
  }
}
