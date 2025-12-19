package com.loopers.infrastructure.kafka.producer;

import com.loopers.infrastructure.kafka.dto.StockChangedDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockChangedEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topic.product-stock-name}")
    private String stockChangedTopic;

    @Retry(name = "kafkaProducer", fallbackMethod = "stockChangedFallback")
    public void sendStockChangedEvent(Long productId, int stock, String changedType) {
        StockChangedDto event = StockChangedDto.of(productId, stock, changedType);
        kafkaTemplate.send(stockChangedTopic, productId.toString(), event);
        log.info("재고 변경 이벤트 발행: productId={}, stock={}, changedType={}",
                productId, stock, changedType);
    }

    public void stockChangedFallback(Long productId, int stock, String changedType, Throwable ex) {
        log.error("재고 변경 이벤트 발행 실패 (재시도 후): productId={}, stock={}, changedType={}",
                productId, stock, changedType, ex);
    }
}
