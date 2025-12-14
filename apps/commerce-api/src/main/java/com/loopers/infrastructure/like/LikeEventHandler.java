package com.loopers.infrastructure.like;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.loopers.domain.like.event.LikeChangedEvent;
import com.loopers.domain.product.ProductMVService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 좋아요 도메인 이벤트 핸들러 (Infrastructure Layer)
 * <p>
 * 도메인 엔티티에서 발행된 좋아요 이벤트를 처리합니다.
 * Week 7 요구사항: 집계 로직의 성공/실패와 상관없이 좋아요 처리는 정상 완료되어야 함
 *
 * @author hyunjikoh
 * @since 2025. 12. 10.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeEventHandler {

    private final ProductMVService productMVService;

    /**
     * 좋아요 변경 이벤트 처리
     * <p>
     * AFTER_COMMIT + @Async로 좋아요 트랜잭션과 완전 분리
     * 집계 업데이트 실패가 좋아요 처리에 영향 주지 않음
     *
     * @param event 좋아요 변경 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleLikeChanged(LikeChangedEvent event) {
        try {
            log.debug("좋아요 집계 업데이트 시작 - productId: {}, action: {}, delta: {}",
                    event.productId(), event.action(), event.countDelta());

            // MV 테이블의 좋아요 카운트 업데이트
            productMVService.updateLikeCount(event.productId(), event.countDelta());

            log.debug("좋아요 집계 업데이트 완료 - productId: {}, delta: {}",
                    event.productId(), event.countDelta());

        } catch (Exception e) {
            // 집계 업데이트 실패해도 좋아요 처리에는 영향 없음
            log.error("좋아요 집계 업데이트 실패 - productId: {}, action: {}, delta: {}",
                    event.productId(), event.action(), event.countDelta(), e);
        }
    }
}
