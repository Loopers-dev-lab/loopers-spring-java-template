package com.loopers.infrastructure.consumer;

import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.infrastructure.idempotent.EventHandled;
import com.loopers.infrastructure.idempotent.EventHandledRepository;
import com.loopers.infrastructure.metrics.ProductMetrics;
import com.loopers.infrastructure.metrics.ProductMetricsRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
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
public class ProductLikedConsumer {

    private final EventHandledRepository eventHandledRepository;
    private final ProductMetricsRepository productMetricsRepository;
    private final RankingRedisService rankingRedisService;

    @KafkaListener(topics = "product-liked", groupId = "product-liked-consumer")
    @Transactional
    public void consume(
            @Payload ProductLikedEvent event,
            @Header("outbox-id") String eventId,
            Acknowledgment ack
    ) {
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("이미 처리된 이벤트 무시: eventId={}", eventId);
            ack.acknowledge();
            return;
        }

        try {
            processProductLikedEvent(event);
            eventHandledRepository.save(EventHandled.create(eventId));
            ack.acknowledge();
            log.info("좋아요 이벤트 처리 완료: eventId={}, productId={}", eventId, event.getProductId());
        } catch (Exception e) {
            log.error("좋아요 이벤트 처리 실패, 재처리 예정: eventId={}", eventId, e);
        }
    }

    private void processProductLikedEvent(ProductLikedEvent event) {
        ProductMetrics metrics = productMetricsRepository.findByProductId(event.getProductId())
                .orElseGet(() -> ProductMetrics.create(event.getProductId()));

        if (metrics.updateLikeIfNewer(event.isLiked(), event.getOccurredAt())) {
            productMetricsRepository.save(metrics);
        }

        if (event.isLiked()) {
            rankingRedisService.incrementScoreForLike(event.getOccurredAt().toLocalDate(), event.getProductId());
        }
    }
}
