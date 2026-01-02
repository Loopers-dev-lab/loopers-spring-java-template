package com.loopers.application.like;

import com.loopers.application.eventhandled.EventHandledFacade;
import com.loopers.application.metrics.ProductMetricsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeEventHandler {

    private final EventHandledFacade eventHandledFacade;
    private final ProductMetricsFacade productMetricsFacade;

    /**
     * 좋아요 추가 이벤트 처리
     */
    @Transactional
    public void handleLikeAdded(String eventId, Long productId) {
        // 멱등성 체크
        if (eventHandledFacade.isAlreadyHandled(eventId)) {
            log.info("이미 처리된 이벤트 - eventId: {}", eventId);
            return;
        }

        // 좋아요 집계 증가
        productMetricsFacade.incrementLikeCount(productId);

        // 이벤트 처리 완료 기록
        eventHandledFacade.markAsHandled(eventId, "LikeAdded", "PRODUCT_LIKE", productId.toString());

        log.info("좋아요 추가 이벤트 처리 완료 - eventId: {}, productId: {}", eventId, productId);
    }

    /**
     * 좋아요 취소 이벤트 처리
     */
    @Transactional
    public void handleLikeRemoved(String eventId, Long productId) {
        // 멱등성 체크
        if (eventHandledFacade.isAlreadyHandled(eventId)) {
            log.info("이미 처리된 이벤트 - eventId: {}", eventId);
            return;
        }

        // 좋아요 집계 감소
        productMetricsFacade.decrementLikeCount(productId);

        // 이벤트 처리 완료 기록
        eventHandledFacade.markAsHandled(eventId, "LikeRemoved", "PRODUCT_LIKE", productId.toString());

        log.info("좋아요 취소 이벤트 처리 완료 - eventId: {}, productId: {}", eventId, productId);
    }
}
