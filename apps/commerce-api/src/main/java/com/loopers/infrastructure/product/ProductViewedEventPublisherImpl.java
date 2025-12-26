package com.loopers.infrastructure.product;

import com.loopers.domain.event.outbox.OutboxEventService;
import com.loopers.domain.product.ProductViewedEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 상품 조회 이벤트를 Kafka로 발행하기 위한 Publisher
 * 
 * 처리 플로우:
 * 1. ProductViewed 이벤트를 받아서 Outbox 테이블에 저장 (같은 트랜잭션)
 * 2. OutboxPublisher가 주기적으로 Kafka로 발행
 * 3. Consumer가 Kafka에서 수신하여 viewCount 집계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedEventPublisherImpl implements ProductViewedEventPublisher {
    private final OutboxEventService outboxEventService;

    @Override
    public void publishProductViewed(Long productId) {
        if (productId == null) {
            log.warn("ProductId가 null입니다. 이벤트 발행을 건너뜁니다.");
            return;
        }

        try {
            // 이벤트 페이로드 생성
            Map<String, Object> eventPayload = createEventPayload(productId);
            
            // Outbox 테이블에 저장 (도메인 트랜잭션 내에서 호출되어야 함)
            outboxEventService.saveEvent(
                    "PRODUCT",
                    productId.toString(),
                    "ProductViewed",
                    eventPayload
            );
            
            log.debug("ProductViewedEvent saved to outbox: productId={}", productId);
        } catch (Exception e) {
            log.error("Failed to save ProductViewedEvent to outbox: productId={}", productId, e);
            throw e;
        }
    }

    /**
     * ProductViewed 이벤트를 Kafka 이벤트 페이로드로 변환
     */
    private Map<String, Object> createEventPayload(Long productId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", "ProductViewed");
        payload.put("aggregateId", productId.toString());
        payload.put("productId", productId);
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }
}

