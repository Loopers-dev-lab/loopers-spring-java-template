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

import java.time.LocalDate;

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
        LocalDate eventDate = event.getOccurredAt().toLocalDate();

        // 1. 레코드가 없으면 생성 (like_count = 0)
        productMetricsRepository.upsertLikeCount(event.getProductId(), eventDate, 0);

        // 2. Pessimistic Lock으로 조회
        ProductMetrics metrics = productMetricsRepository.findByProductIdAndDateForUpdate(event.getProductId(), eventDate)
                .orElseThrow(() -> new IllegalStateException("ProductMetrics should exist after upsert"));

        // 3. 비즈니스 로직 수행
        if (metrics.updateLikeIfNewer(event.isLiked(), event.getOccurredAt())) {
            productMetricsRepository.save(metrics);
        }

        if (event.isLiked()) {
            rankingRedisService.incrementScoreForLike(eventDate, event.getProductId());
        }
    }
}
