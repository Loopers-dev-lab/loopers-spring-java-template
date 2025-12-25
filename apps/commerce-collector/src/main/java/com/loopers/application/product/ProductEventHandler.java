package com.loopers.application.product;

import com.loopers.domain.eventhandled.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
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

    private final EventHandledService eventHandledService;
    private final ProductMetricsService productMetricsService;

    @Transactional
    public void handleProductViewed(String eventId, Long productId) {
        // 1. 멱등성 체크
        if (eventHandledService.isAlreadyHandled(eventId)) {
            log.info("이미 처리된 상품 상세 조회 이벤트 - eventId: {}", eventId);
            return;
        }

        // 2. 상품 상세 집계 증가
        productMetricsService.incrementViewCount(productId);
        log.info("상품 상세 집계 증가 - productId: {}", productId);

        // 3. 이벤트 처리 완료 기록
        eventHandledService.markAsHandled(
                eventId, ProductDetail.PRODUCT_VIEWED, AggregateTypes.PRODUCT_VIEW, productId.toString()
        );

        log.info("상품 상세 조회 이벤트 처리 완료 - eventId: {}, productId: {}",
                eventId, productId);
    }
}
