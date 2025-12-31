package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.productmetrics.ProductMetricsService;
import com.loopers.messaging.event.KafkaEventMessage;
import com.loopers.support.event.KafkaEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsConsumer {

    private final KafkaEventProcessor eventProcessor;
    private final ProductMetricsService productMetricsService;

    @KafkaListener(
            topics = {"catalog-events"}, // 이 토픽으로 모든 상품 메트릭 이벤트가 들어올 것을 가정
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

    private void handle(KafkaEventMessage<Object> message) {
        LocalDate today = LocalDate.now();
        Long productId = message.getAggregateId();

        // 카운트 원자적 증가
        switch (message.getEventName()) {
            case "LIKE_CREATED" ->
                    productMetricsService.increaseLikeCount(productId, today);
            case "LIKE_CANCELED" ->
                    productMetricsService.decreaseLikeCount(productId, today);
            case "PRODUCT_VIEWED" ->
                    productMetricsService.increaseViewCount(productId, today);
        }
    }

}
