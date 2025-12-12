package com.loopers.application.like.event;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 이벤트 핸들러
 * - 좋아요 토글 후 후속 처리를 비동기로 수행한다
 * - 상품의 좋아요 수 집계 (별도 트랜잭션, eventual consistency)
 * - 캐시 무효화
 * - 사용자 행동 로깅
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventHandler {

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;

    /**
     * 좋아요 토글 후 상품의 좋아요 수 집계 처리
     * - 좋아요 트랜잭션이 커밋된 후 실행된다
     * - 별도 트랜잭션으로 실행되어 집계 처리 실패가 좋아요에 영향을 주지 않는다
     * - eventual consistency: 좋아요 수가 즉시 반영되지 않을 수 있다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void updateLikeCount(LikeToggledEvent event) {
        try {
            log.info("[이벤트] 좋아요 수 집계 시작: productId={}, userId={}, isAdded={}",
                    event.productId(), event.userId(), event.isAdded());

            Product product = productRepository.findById(event.productId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: " + event.productId()));

            if (event.isAdded()) {
                product.incrementLikeCount();
            } else {
                product.decrementLikeCount();
            }

            productRepository.save(product);

            log.info("[이벤트] 좋아요 수 집계 완료: productId={}, likeCount={}",
                    event.productId(), product.getLikeCount());
        } catch (Exception e) {
            // 집계 실패는 로그만 남기고 좋아요는 유지한다
            // 스케줄러로 주기적으로 동기화하거나 재시도 로직으로 복구 가능
            log.error("[이벤트] 좋아요 수 집계 실패: productId={}, userId={}, error={}",
                    event.productId(), event.userId(), e.getMessage(), e);
        }
    }

    /**
     * 좋아요 토글 후 캐시 무효화
     * - 좋아요 트랜잭션이 커밋된 후 실행된다
     * - 비동기로 실행되어 좋아요 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void evictCache(LikeToggledEvent event) {
        try {
            log.info("[이벤트] 캐시 무효화 시작: productId={}", event.productId());

            productCacheService.evictProductDetail(event.productId());
            productCacheService.evictProductList();

            log.info("[이벤트] 캐시 무효화 완료: productId={}", event.productId());
        } catch (Exception e) {
            log.error("[이벤트] 캐시 무효화 실패: productId={}, error={}",
                    event.productId(), e.getMessage(), e);
        }
    }

    /**
     * 좋아요 토글 시 사용자 행동 로깅
     * - 좋아요 토글 후 즉시 실행된다
     * - 비동기로 실행되어 좋아요 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void logUserAction(LikeToggledEvent event) {
        try {
            String action = event.isAdded() ? "좋아요 추가" : "좋아요 취소";
            log.info("[사용자 행동 로그] {}: userId={}, productId={}",
                    action, event.userId(), event.productId());

            // TODO: 실제 로깅 시스템 연동 (Elasticsearch, CloudWatch 등)
        } catch (Exception e) {
            log.error("[이벤트] 사용자 행동 로깅 실패: productId={}, error={}",
                    event.productId(), e.getMessage());
        }
    }
}
