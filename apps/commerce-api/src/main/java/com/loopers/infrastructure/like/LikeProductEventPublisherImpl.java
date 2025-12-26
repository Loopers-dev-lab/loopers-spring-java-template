package com.loopers.infrastructure.like;

import com.loopers.domain.event.outbox.OutboxEventService;
import com.loopers.domain.like.product.LikeProductEvent;
import com.loopers.domain.like.product.LikeProductEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 좋아요 이벤트를 Kafka로 발행하기 위한 Publisher
 * 
 * 변경 사항:
 * - 기존: ApplicationEventPublisher를 통한 동기식 이벤트 발행
 * - 변경: Outbox 패턴을 통한 비동기 Kafka 이벤트 발행
 * 
 * 처리 플로우:
 * 1. LikeProductEvent를 받아서 Outbox 테이블에 저장 (같은 트랜잭션)
 * 2. OutboxPublisher가 주기적으로 Kafka로 발행
 * 3. Consumer가 Kafka에서 수신하여 Metrics 집계
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeProductEventPublisherImpl implements LikeProductEventPublisher {
    private final OutboxEventService outboxEventService;

    @Override
    public void publishLikeEvent(LikeProductEvent event) {
        if (event == null) {
            log.warn("LikeProductEvent가 null입니다. 이벤트 발행을 건너뜁니다.");
            return;
        }

        try {
            // 이벤트 페이로드 생성
            Map<String, Object> eventPayload = createEventPayload(event);
            
            // 이벤트 타입 결정
            String eventType = event.getLiked() ? "ProductLiked" : "ProductUnliked";
            
            // Outbox 테이블에 저장 (도메인 트랜잭션 내에서 호출되어야 함)
            outboxEventService.saveEvent(
                    "PRODUCT",
                    event.getProductId().toString(),
                    eventType,
                    eventPayload
            );
            
            log.info("LikeProductEvent saved to outbox: productId={}, liked={}, eventType={}", 
                    event.getProductId(), event.getLiked(), eventType);
        } catch (Exception e) {
            log.error("Failed to save LikeProductEvent to outbox: productId={}, liked={}", 
                    event.getProductId(), event.getLiked(), e);
            throw e;
        }
    }

    /**
     * LikeProductEvent를 Kafka 이벤트 페이로드로 변환
     */
    private Map<String, Object> createEventPayload(LikeProductEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("eventType", event.getLiked() ? "ProductLiked" : "ProductUnliked");
        payload.put("aggregateId", event.getProductId().toString());
        payload.put("productId", event.getProductId());
        payload.put("userId", event.getUserId());
        payload.put("brandId", event.getBrandId());
        payload.put("liked", event.getLiked());
        payload.put("createdAt", java.time.Instant.now().toString());
        return payload;
    }
}
