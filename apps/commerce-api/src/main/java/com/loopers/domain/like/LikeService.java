package com.loopers.domain.like;

import com.loopers.application.event.LikeEvent;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeService {

  private final LikeRepository likeRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void save(Long userId, Long productId) {
    Optional<Like> liked = likeRepository.findById(userId, productId);
    if (!liked.isPresent()) {
      likeRepository.save(userId, productId);
    }

    // 좋아요 이벤트 발행 (배치 처리용)
    LikeEvent likeEvent = new LikeEvent(userId, productId, LikeEvent.LikeAction.LIKE);
    eventPublisher.publishEvent(likeEvent);
    log.debug("좋아요 이벤트 발행 - 사용자ID: {}, 상품ID: {}", userId, productId);
  }


  @Transactional
  public void remove(Long userId, Long productId) {

    likeRepository.remove(userId, productId);

    // 좋아요 취소 이벤트 발행 (배치 처리용)
    LikeEvent unlikeEvent = new LikeEvent(userId, productId, LikeEvent.LikeAction.UNLIKE);
    eventPublisher.publishEvent(unlikeEvent);
    log.debug("좋아요 취소 이벤트 발행 - 사용자ID: {}, 상품ID: {}", userId, productId);
  }

  @Transactional(readOnly = true)
  public boolean isLiked(Long userId, Long productId) {
    return likeRepository.isLiked(userId, productId);
  }

  @Transactional(readOnly = true)
  public long getLikeCount(Long productId) {
    return likeRepository.getLikeCount(productId);
  }


  @Transactional(readOnly = true)
  public List<ProductIdAndLikeCount> getLikeCount(List<Long> productIds) {
    return likeRepository.getLikeCountWithProductId(productIds);
  }

  public Page<Product> getLikedProducts(Long userId, String sortType, int page, int size) {
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID가 없습니다.");
    }
    Sort sort = this.getSortBySortType(sortType);
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<Like> likePage = likeRepository.getLikedProducts(userId, pageable);
    List<Product> likedProducts = likePage.getContent().stream().map(Like::getProduct).toList();
    return new PageImpl<>(likedProducts, pageable, likePage.getTotalElements());
  }

  private Sort getSortBySortType(String sortType) {
    if (sortType == null) sortType = "latest";
    Sort latestSort = Sort.by("createdAt").descending();
    switch (sortType.toLowerCase()) {
      case "latest":
        return latestSort;
      case "product_asc":
        return Sort.by("refProductId").ascending().and(latestSort);
      default:
        return latestSort;
    }
  }
}
