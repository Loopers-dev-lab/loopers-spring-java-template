package com.loopers.interfaces.consumer;

import com.loopers.application.ProductCacheService;
import com.loopers.application.ProductMetricsService;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"stock.deducted.v1", "stock.compensated.v1"},
        groupId = "commerce-streamer-stock-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class StockEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final ProductCacheService productCacheService;
    private final ProductMetricsService productMetricsService;

    @KafkaHandler
    public void handleProcessed(ConsumerRecord<String, StockEvents.Processed> record, Acknowledgment ack) {
        log.info("Received StockEvents.Processed for orderId: {}", record.value().orderId());

        messageProcessor.execute(record, ack, "stock", event -> {
            if (event.orderItems() != null) {
                event.orderItems().forEach(item -> {
                    productCacheService.evictProductCache(item.productId());
                    productMetricsService.upsertSalesCount(item.productId(), item.quantity()); // 판매량 업데이트
                });
            }
        });
    }

    @KafkaHandler
    public void handleCompensated(ConsumerRecord<String, StockEvents.Compensated> record, Acknowledgment ack) {
        log.info("Received StockEvents.Compensated for orderId: {}. Cache eviction skipped due to missing item info.", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "stock", event -> {
            // 보상 이벤트는 추가 처리 없음
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in stock topics: {}", record.value());
        ack.acknowledge();
    }
}
