package com.loopers.application.like;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.application.event.tracking.UserActionEvent;
import com.loopers.domain.like.service.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void likeProduct(Long memberId, Long productId) {
        // 1. DB에 좋아요 저장 (Redis 로직은 LikeService에서 제거됨)
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
        // 1. DB에서 좋아요 삭제 (Redis 로직은 LikeService에서 제거됨)
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
