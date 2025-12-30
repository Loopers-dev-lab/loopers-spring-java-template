package com.loopers.infrastructure.like;

import com.loopers.domain.like.event.LikeEvent;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.product.ProductRepository;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeAggregationEventListener {

    private static final String CATALOG_TOPIC = "catalog-events";
    private static final String EVENT_PRODUCT_LIKED = "PRODUCT_LIKED";
    private static final String EVENT_PRODUCT_UNLIKED = "PRODUCT_UNLIKED";

    private final ProductRepository productRepository;
    private final OutboxService outboxService;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LikeEvent.ProductLiked event) {
        try {
            productRepository.incrementLikeCount(event.productId());
            publishCatalogEvent(EVENT_PRODUCT_LIKED, event.userId(), event.productId(), 1);
        } catch (Exception exception) {
            log.error("집계 처리 실패 - productId={}, type=LIKE", event.productId(), exception);
        }
    }

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LikeEvent.ProductUnliked event) {
        try {
            productRepository.decrementLikeCount(event.productId());
            publishCatalogEvent(EVENT_PRODUCT_UNLIKED, event.userId(), event.productId(), -1);
        } catch (Exception exception) {
            log.error("집계 처리 실패 - productId={}, type=UNLIKE", event.productId(), exception);
        }
    }

    private void publishCatalogEvent(String eventType, String userId, Long productId, long delta) {
        ZonedDateTime occurredAt = ZonedDateTime.now();
        String eventId = outboxService.nextEventId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", eventType);
        payload.put("productId", productId);
        payload.put("userId", userId);
        payload.put("delta", delta);
        payload.put("occurredAt", occurredAt);
        outboxService.append(
                eventId,
                CATALOG_TOPIC,
                productId.toString(),
                eventType,
                payload,
                occurredAt
        );
    }
}
