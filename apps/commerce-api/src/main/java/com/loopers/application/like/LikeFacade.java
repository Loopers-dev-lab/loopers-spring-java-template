package com.loopers.application.like;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.tracking.UserActionEvent;
import com.loopers.domain.like.repository.LikeRepository;
import com.loopers.domain.like.service.LikeService;
import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
@Transactional
public class LikeFacade {

    private final LikeService likeService;
    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void likeProduct(Long memberId, Long productId) {
        // 멱등성: 이미 좋아요한 경우 early return (좋아요가 존재하면 상품도 존재함)
        if (likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            log.debug("[LikeFacade] 이미 좋아요한 상품 - memberId: {}, productId: {}", memberId, productId);
            return;
        }

        // 1. DB에 좋아요 저장
        Product product = likeService.like(memberId, productId);

        // 2. 이벤트 발행 (Redis 업데이트는 비동기 리스너에서 처리)
        eventPublisher.publishEvent(new ProductLikedEvent(
            memberId,
            productId,
            product.getBrandId(),
            LocalDateTime.now()
        ));

        eventPublisher.publishEvent(UserActionEvent.of(
            "PRODUCT_LIKE",
            memberId,
            "PRODUCT",
            String.valueOf(productId),
            Map.of("brandId", product.getBrandId())
        ));

        log.info("[LikeFacade] 좋아요 이벤트 발행 - memberId: {}, productId: {}", memberId, productId);
    }

    public void unlikeProduct(Long memberId, Long productId) {
        // 멱등성: 좋아요하지 않은 경우 early return
        if (!likeRepository.existsByMemberIdAndProductId(memberId, productId)) {
            log.debug("[LikeFacade] 좋아요하지 않은 상품 - memberId: {}, productId: {}", memberId, productId);
            return;
        }

        // 1. DB에서 좋아요 삭제
        Product product = likeService.unlike(memberId, productId);

        // 2. 이벤트 발행 (Redis 업데이트는 비동기 리스너에서 처리)
        eventPublisher.publishEvent(new ProductUnlikedEvent(
            memberId,
            productId,
            product.getBrandId(),
            LocalDateTime.now()
        ));

        eventPublisher.publishEvent(UserActionEvent.of(
            "PRODUCT_UNLIKE",
            memberId,
            "PRODUCT",
            String.valueOf(productId),
            Map.of("brandId", product.getBrandId())
        ));

        log.info("[LikeFacade] 좋아요 취소 이벤트 발행 - memberId: {}, productId: {}", memberId, productId);
    }
}
