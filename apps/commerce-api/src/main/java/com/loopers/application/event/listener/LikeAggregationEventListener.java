package com.loopers.application.event.listener;

import com.loopers.application.event.like.ProductLikedEvent;
import com.loopers.application.event.like.ProductUnlikedEvent;
import com.loopers.infrastructure.cache.CacheInvalidationService;
import com.loopers.infrastructure.cache.MemberLikesCache;
import com.loopers.infrastructure.cache.ProductLikeCountCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 좋아요 집계 이벤트 리스너
 *
 * - 좋아요/취소 이벤트를 수신하여 Redis 캐시를 비동기로 업데이트
 * - eventual consistency: DB 저장 후 Redis는 비동기로 업데이트
 * - Redis 실패 시에도 DB는 정상 저장됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeAggregationEventListener {

    private final ProductLikeCountCache productLikeCountCache;
    private final MemberLikesCache memberLikesCache;
    private final CacheInvalidationService cacheInvalidationService;

    /**
     * 좋아요 이벤트 처리 (비동기 + 재시도)
     * - Redis 좋아요 카운트 증가
     * - 회원 좋아요 목록 캐시 업데이트
     * - 상품 캐시 무효화
     *
     * 재시도 전략:
     * - 최대 3회 재시도
     * - 초기 딜레이 100ms, 지수 백오프 (100ms → 200ms → 400ms)
     * - 일시적 Redis 네트워크 오류 대응
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2),
        recover = "recoverProductLiked"
    )
    public void handleProductLiked(ProductLikedEvent event) {
        log.info("[LikeAggregationEventListener] 좋아요 이벤트 처리 - memberId: {}, productId: {}",
                 event.memberId(), event.productId());

        // 재시도 가능하도록 try-catch 제거 (예외를 위로 전파)
        productLikeCountCache.increment(event.productId());
        memberLikesCache.add(event.memberId(), event.productId());
        cacheInvalidationService.invalidateOnLikeChange(event.productId(), event.brandId());

        log.debug("[LikeAggregationEventListener] 좋아요 집계 완료 - productId: {}", event.productId());
    }

    /**
     * 좋아요 취소 이벤트 처리 (비동기 + 재시도)
     * - Redis 좋아요 카운트 감소
     * - 회원 좋아요 목록 캐시 업데이트
     * - 상품 캐시 무효화
     *
     * 재시도 전략:
     * - 최대 3회 재시도
     * - 초기 딜레이 100ms, 지수 백오프 (100ms → 200ms → 400ms)
     * - 일시적 Redis 네트워크 오류 대응
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2),
        recover = "recoverProductUnliked"
    )
    public void handleProductUnliked(ProductUnlikedEvent event) {
        log.info("[LikeAggregationEventListener] 좋아요 취소 이벤트 처리 - memberId: {}, productId: {}",
                 event.memberId(), event.productId());

        // 재시도 가능하도록 try-catch 제거 (예외를 위로 전파)
        productLikeCountCache.decrement(event.productId());
        memberLikesCache.remove(event.memberId(), event.productId());
        cacheInvalidationService.invalidateOnLikeChange(event.productId(), event.brandId());

        log.debug("[LikeAggregationEventListener] 좋아요 취소 집계 완료 - productId: {}", event.productId());
    }

    /**
     * 좋아요 집계 최종 실패 시 복구 메서드
     * - 3회 재시도 후에도 실패한 경우 호출됨
     * - 로그 기록 및 향후 DLQ 저장 가능
     */
    @Recover
    public void recoverProductLiked(Exception ex, ProductLikedEvent event) {
        log.error("[LikeAggregationEventListener] 좋아요 집계 최종 실패 - memberId: {}, productId: {}, error: {}",
                event.memberId(), event.productId(), ex.getMessage(), ex);

        // TODO: Dead Letter Queue에 저장하여 나중에 재처리
        // deadLetterQueueService.save(event, ex);

        // TODO: 알림 전송 (심각한 Redis 장애)
        // alertService.sendAlert("좋아요 집계 실패", event, ex);
    }

    /**
     * 좋아요 취소 집계 최종 실패 시 복구 메서드
     * - 3회 재시도 후에도 실패한 경우 호출됨
     * - 로그 기록 및 향후 DLQ 저장 가능
     */
    @Recover
    public void recoverProductUnliked(Exception ex, ProductUnlikedEvent event) {
        log.error("[LikeAggregationEventListener] 좋아요 취소 집계 최종 실패 - memberId: {}, productId: {}, error: {}",
                event.memberId(), event.productId(), ex.getMessage(), ex);

        // TODO: Dead Letter Queue에 저장하여 나중에 재처리
        // deadLetterQueueService.save(event, ex);

        // TODO: 알림 전송 (심각한 Redis 장애)
        // alertService.sendAlert("좋아요 취소 집계 실패", event, ex);
    }
}
