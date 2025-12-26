package com.loopers.infrastructure.consumer;

import com.loopers.domain.product.event.StockDepletedEvent;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.idempotent.EventHandled;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDepletedConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final ProductCacheService productCacheService;

    @KafkaListener(topics = "stock-depleted", groupId = "stock-depleted-consumer")
    @Transactional
    public void consume(
            @Payload StockDepletedEvent event,
            @Header("outbox-id") String eventId,
            Acknowledgment ack
    ) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("이미 처리된 이벤트 무시: eventId={}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            processStockDepletedEvent(event);
            eventHandledRepository.save(EventHandled.create(eventId));
            ack.acknowledge();
            log.info("재고 소진 이벤트 처리 완료: eventId={}, productId={}", eventId, event.getProductId());
        } catch (Exception e) {
            log.error("재고 소진 이벤트 처리 실패, 재처리 예정: eventId={}", eventId, e);
        }
    }

    private void processStockDepletedEvent(StockDepletedEvent event) {
        productCacheService.deleteProductDetail(event.getProductId());
        productCacheService.invalidateProductListCaches();
        log.info("재고 소진으로 캐시 무효화: productId={}", event.getProductId());
    }
}
