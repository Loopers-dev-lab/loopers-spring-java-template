package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.application.event.LikeEvent;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.outbox.OutboxSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeFacade {
  private final LikeService likeService;
  private final LikeCacheRepository likeCacheRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final OutboxService outboxService;

  public LikeInfo like(Long userId, Long productId) {
    Boolean userLiked = likeCacheRepository.getUserLiked(userId, productId);
    if (Boolean.TRUE.equals(userLiked)) {
      Long count = likeCacheRepository.getLikeCount(productId);
      count = (count == null ? 1 : count);
      return LikeInfo.from(count, true);
    }
    likeCacheRepository.putUserLiked(userId, productId, true);
    Long newCount = likeCacheRepository.addLikeCount(productId);
    likeService.save(userId, productId);

    // Outbox 이벤트 저장 (배치 처리용)
    LikeEvent likeEvent = new LikeEvent(userId, productId, LikeEvent.LikeAction.LIKE);
    outboxService.saveEvent(
        "Product",
        productId.toString(),
        "ProductLiked",
        likeEvent
    );

    // 기존 동기 이벤트도 유지 (내부 처리용)
    eventPublisher.publishEvent(likeEvent);
    log.debug("좋아요 이벤트 발행 - 사용자ID: {}, 상품ID: {}", userId, productId);

    return LikeInfo.from(newCount, true);
  }

  public LikeInfo unlike(Long userId, Long productId) {
    likeCacheRepository.evictUserLike(userId, productId);

    Long cachedCount = likeCacheRepository.subtractLikeCount(productId);
    cachedCount = (cachedCount == null ? 0 : cachedCount);

    likeService.remove(userId, productId);

    // Outbox 이벤트 저장 (배치 처리용)
    LikeEvent unlikeEvent = new LikeEvent(userId, productId, LikeEvent.LikeAction.UNLIKE);
    outboxService.saveEvent(
        "Product",
        productId.toString(),
        "ProductUnliked",
        unlikeEvent
    );

    // 기존 동기 이벤트도 유지 (내부 처리용)
    eventPublisher.publishEvent(unlikeEvent);
    log.debug("좋아요 취소 이벤트 발행 - 사용자ID: {}, 상품ID: {}", userId, productId);

    return LikeInfo.from(cachedCount, false);
  }

  public LikeInfo getLikeInfo(Long userId, Long productId) {
    Boolean liked = likeCacheRepository.getUserLiked(userId, productId);
    if (liked == null) {
      liked = likeService.isLiked(userId, productId);
      likeCacheRepository.putUserLiked(userId, productId, liked);
    }

    Long likeCount = likeCacheRepository.getLikeCount(productId);
    if (likeCount == null) {
      likeCount = likeService.getLikeCount(productId);
      likeCacheRepository.putLikeCount(productId, likeCount);
    }

    return LikeInfo.from(likeCount, liked);
  }
}
