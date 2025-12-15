package com.loopers.application.like;

import com.loopers.application.outbox.OutboxEventService;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final OutboxEventService outboxEventService;

    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public void like(String userId, Long productId) {
        log.info("좋아요 처리 시작 - userId: {}, productId: {}", userId, productId);

        // 멱등성 보장: 이미 좋아요한 경우 무시
        if (likeRepository.existsByUserIdAndProductId(userId, productId)) {
            log.info("이미 좋아요한 상품 - userId: {}, productId: {}", userId, productId);
            return;
        }

        // 상품 존재 확인 (락 없이)
        productRepository.findById(productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

        // 좋아요 저장
        Like like = Like.builder()
            .userId(userId)
            .productId(productId)
            .build();
        Like savedLike = likeRepository.save(like);
        log.info("좋아요 저장 완료 - likeId: {}", savedLike.getId());

        // 이벤트를 Outbox에 저장 (집계는 이벤트 핸들러에서)
        LikeCreatedEvent event = LikeCreatedEvent.from(savedLike);
        outboxEventService.saveEvent("LIKE", savedLike.getId().toString(),
            "LikeCreatedEvent", event);
        log.info("좋아요 생성 이벤트 Outbox 저장 - likeId: {}", savedLike.getId());
    }

    @Transactional
    @CacheEvict(value = "product", key = "#productId")
    public void unlike(String userId, Long productId) {
        log.info("좋아요 취소 시작 - userId: {}, productId: {}", userId, productId);

        // 멱등성 보장: 좋아요하지 않은 경우에도 정상 처리
        if (!likeRepository.existsByUserIdAndProductId(userId, productId)) {
            log.info("좋아요하지 않은 상품 - userId: {}, productId: {}", userId, productId);
            return;
        }

        // 좋아요 삭제
        likeRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("좋아요 삭제 완료 - userId: {}, productId: {}", userId, productId);

        // 이벤트를 Outbox에 저장
        LikeDeletedEvent event = LikeDeletedEvent.of(userId, productId);
        outboxEventService.saveEvent("LIKE", productId.toString(),
            "LikeDeletedEvent", event);
        log.info("좋아요 삭제 이벤트 Outbox 저장 - userId: {}, productId: {}", userId, productId);
    }

    public Page<Like> getLikesByUser(String userId, Pageable pageable) {
        return likeRepository.findByUserId(userId, pageable);
    }

    public Long getLikeCount(Long productId) {
        return likeRepository.countByProductId(productId);
    }

    public boolean isLiked(String userId, Long productId) {
        return likeRepository.existsByUserIdAndProductId(userId, productId);
    }
}
