package com.loopers.infrastructure.event;

import com.loopers.domain.product.event.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewEventPublisher {

    private static final String TOPIC = "product-viewed";

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void publish(Long productId) {
        try {
            ProductViewedEvent event = ProductViewedEvent.of(productId);
            kafkaTemplate.send(TOPIC, String.valueOf(productId), event);
            log.debug("조회 이벤트 발행: productId={}", productId);
        } catch (Exception e) {
            log.warn("조회 이벤트 발행 실패: productId={}, error={}", productId, e.getMessage());
        }
    }
}
