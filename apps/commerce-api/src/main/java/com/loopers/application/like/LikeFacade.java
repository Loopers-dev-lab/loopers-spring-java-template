package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import com.loopers.application.event.LikeEvent;
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

    eventPublisher.publishEvent(new LikeEvent(userId, productId, LikeEvent.LikeAction.LIKE));
    log.debug("좋아요 이벤트 발행 - 사용자ID: {}, 상품ID: {}", userId, productId);

    return LikeInfo.from(newCount, true);
  }

  public LikeInfo unlike(Long userId, Long productId) {
    likeCacheRepository.evictUserLike(userId, productId);

    Long cachedCount = likeCacheRepository.subtractLikeCount(productId);
    cachedCount = (cachedCount == null ? 0 : cachedCount);

    likeService.remove(userId, productId);
    
    eventPublisher.publishEvent(new LikeEvent(userId, productId, LikeEvent.LikeAction.UNLIKE));
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
