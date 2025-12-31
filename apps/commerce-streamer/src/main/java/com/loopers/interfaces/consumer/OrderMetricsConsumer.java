package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.cache.CacheKeyService;
import com.loopers.cache.CacheService;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.productmetrics.ProductMetricsService;
import com.loopers.messaging.event.KafkaEventMessage;
import com.loopers.support.event.KafkaEventProcessor;
import com.loopers.support.event.OrderPaidPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMetricsConsumer {

    private final ObjectMapper objectMapper;
    private final ProductMetricsService metricsService;

    private final CacheService cacheService;
    private final CacheKeyService cacheKeyService;

    private final KafkaEventProcessor eventProcessor;

    @KafkaListener(
            topics = "order-events",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment acknowledgment
    ) {
        log.info("catalog-events 배치 수신. 메시지 수: {}", records.size());

        for (ConsumerRecord<Object, Object> record : records) {
            String eventJson = (String) record.value();
            try {
                eventProcessor.process(this::handle, eventJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // manual ack
        acknowledgment.acknowledge();
    }

    public void handle(KafkaEventMessage<OrderPaidPayload> message) {
        OrderPaidPayload payload = objectMapper.convertValue(
            message.getPayload(),
            OrderPaidPayload.class
        );

        for (OrderPaidPayload.OrderItem item : payload.items()) {
            metricsService.increaseSalesCount(
                    item.productId(),
                    item.quantity(),
                    item.totalPrice()
            );
            if (item.soldOut()) {
                invalidateProductCaches(item.productId());
            }
        }
    }

    private void invalidateProductCaches(Long productId) {
        String key = cacheKeyService.productDetailKey(productId);
        cacheService.delete(key);
    }
}
