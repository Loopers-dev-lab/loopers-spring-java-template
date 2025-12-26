package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.event.EventHandled;
import com.loopers.domain.event.EventHandledRepository;
import com.loopers.domain.productmetrics.ProductMetricsService;
import com.loopers.messaging.event.KafkaEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsConsumer {

    private final ObjectMapper objectMapper;
    private final ProductMetricsService productMetricsService;
    private final EventHandledRepository eventHandledRepository;

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
            handle(record);
        }
        // manual ack
        acknowledgment.acknowledge();
    }



    private void handle(ConsumerRecord<Object, Object> record) {
        String eventJson = (String) record.value();

        try {
            KafkaEventMessage<?> message =
                    objectMapper.readValue(
                            record.value().toString(),
                            KafkaEventMessage.class
                    );

            UUID eventId = message.getEventId();

            // 멱등 처리
            if (eventHandledRepository.existsByEventId(eventId)) {
                log.info("이미 처리된 이벤트입니다. eventId={}", eventId);
                return;
            }

            Long productId = message.getAggregateId();

            // 카운트 원자적 증가
            switch (message.getEventName()) {
                case "LIKE_CREATED" ->
                        productMetricsService.increaseLikeCount(productId);
                case "LIKE_CANCELED" ->
                        productMetricsService.decreaseLikeCount(productId);
                case "PRODUCT_VIEWED" ->
                        productMetricsService.increaseViewCount(productId);
            }

            eventHandledRepository.save(EventHandled.from(eventId));

        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 파싱 오류: {}", eventJson, e);
        } catch (Exception e) {
            log.error("컨슈머 오류 발생. 메시지: {}", eventJson, e);
        }
    }
}
