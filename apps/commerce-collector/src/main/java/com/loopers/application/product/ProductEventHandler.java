package com.loopers.application.product;

import com.loopers.application.eventhandled.EventHandledFacade;
import com.loopers.application.metrics.ProductMetricsFacade;
import com.loopers.kafka.AggregateTypes;
import com.loopers.kafka.KafkaTopics.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventHandler {

    private final EventHandledFacade eventHandledFacade;
    private final ProductMetricsFacade productMetricsFacade;

    @Transactional
    public void handleProductViewed(String eventId, Long productId) {
        // 1. 멱등성 체크
        if (eventHandledFacade.isAlreadyHandled(eventId)) {
            log.info("이미 처리된 상품 상세 조회 이벤트 - eventId: {}", eventId);
            return;
        }

        // 2. 상품 상세 집계 증가
        productMetricsFacade.incrementViewCount(productId);
        log.info("상품 상세 집계 증가 - productId: {}", productId);

        // 3. 이벤트 처리 완료 기록
        eventHandledFacade.markAsHandled(
                eventId, ProductDetail.PRODUCT_VIEWED, AggregateTypes.PRODUCT_VIEW, productId.toString()
        );

        log.info("상품 상세 조회 이벤트 처리 완료 - eventId: {}, productId: {}",
                eventId, productId);
    }
}
