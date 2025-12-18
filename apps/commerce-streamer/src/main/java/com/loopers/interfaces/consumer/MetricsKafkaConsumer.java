package com.loopers.interfaces.consumer;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.metrics.MetricsService;
import com.loopers.infrastructure.event.DomainEventEnvelope;
import com.loopers.infrastructure.event.EventDeserializer;
import com.loopers.infrastructure.event.payloads.LikeActionPayloadV1;
import com.loopers.infrastructure.event.payloads.PaymentSuccessPayloadV1;
import com.loopers.infrastructure.event.payloads.ProductViewPayloadV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메트릭스 Kafka 컨슈머 - 멱등성과 최신성을 보장하는 안전한 이벤트 처리
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsKafkaConsumer {

    private final MetricsService metricsService;
    private final EventDeserializer eventDeserializer;

    @KafkaListener(
            topics = {"catalog-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onCatalogEvents(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment ack) {
        
        log.debug("Processing {} catalog events", records.size());
        
        for (ConsumerRecord<Object, Object> record : records) {
            try {
                processCatalogEvent(record);
            } catch (Exception e) {
                log.error("Failed to process catalog event: {}", record.value(), e);
                // 개별 메시지 실패는 로그만 남기고 계속 진행
                // 전체 배치를 실패시키지 않음
            }
        }

        ack.acknowledge();
        log.debug("Acknowledged {} catalog events", records.size());
    }

    @KafkaListener(
            topics = {"order-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onOrderEvents(
            final List<ConsumerRecord<Object, Object>> records,
            final Acknowledgment ack
    ) {
        
        log.debug("Processing {} order events", records.size());
        
        for (ConsumerRecord<Object, Object> record : records) {
            try {
                processOrderEvent(record);
            } catch (Exception e) {
                log.error("Failed to process order event: {}", record.value(), e);
                // 개별 메시지 실패는 로그만 남기고 계속 진행
            }
        }

        ack.acknowledge();
        log.debug("Acknowledged {} order events", records.size());
    }
    
    private void processCatalogEvent(ConsumerRecord<Object, Object> record) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(record.value());
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", record.value());
            return;
        }

        // 멱등성 체크 - 이미 처리된 이벤트는 무시
        final boolean isFirstTime = metricsService.tryMarkHandled(envelope.eventId());
        if (!isFirstTime) {
            log.debug("Event already processed: {}", envelope.eventId());
            return;
        }

        // 이벤트 타입별 처리
        switch (envelope.eventType()) {
            case "PRODUCT_VIEW" -> handleProductView(envelope);
            case "LIKE_ACTION" -> handleLikeAction(envelope);
            default -> log.debug("Unhandled catalog event type: {}", envelope.eventType());
        }
    }
    
    private void processOrderEvent(ConsumerRecord<Object, Object> record) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(record.value());
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", record.value());
            return;
        }

        // 멱등성 체크
        final boolean isFirstTime = metricsService.tryMarkHandled(envelope.eventId());
        if (!isFirstTime) {
            log.debug("Event already processed: {}", envelope.eventId());
            return;
        }

        // PAYMENT_SUCCESS 이벤트만 처리
        if ("PAYMENT_SUCCESS".equals(envelope.eventType())) {
            handlePaymentSuccess(envelope);
        } else {
            log.debug("Unhandled order event type: {}", envelope.eventType());
        }
    }
    
    private void handleProductView(DomainEventEnvelope envelope) {
        final ProductViewPayloadV1 payload = eventDeserializer.deserializeProductView(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid ProductView payload: {}", envelope.payloadJson());
            return;
        }
        
        metricsService.incrementView(payload.productId(), envelope.occurredAtEpochMillis());
        log.debug("Processed PRODUCT_VIEW for productId: {}", payload.productId());
    }
    
    private void handleLikeAction(DomainEventEnvelope envelope) {
        final LikeActionPayloadV1 payload = eventDeserializer.deserializeLikeAction(envelope.payloadJson());
        if (payload == null || payload.productId() == null || payload.action() == null) {
            log.warn("Invalid LikeAction payload: {}", envelope.payloadJson());
            return;
        }
        
        final int delta = "LIKE".equals(payload.action()) ? 1 : -1;
        metricsService.applyLikeDelta(payload.productId(), delta, envelope.occurredAtEpochMillis());
        log.debug("Processed LIKE_ACTION for productId: {}, action: {}", payload.productId(), payload.action());
    }
    
    private void handlePaymentSuccess(DomainEventEnvelope envelope) {
        final PaymentSuccessPayloadV1 payload = eventDeserializer.deserializePaymentSuccess(envelope.payloadJson());
        if (payload == null || payload.items() == null) {
            log.warn("Invalid PaymentSuccess payload: {}", envelope.payloadJson());
            return;
        }
        
        for (PaymentSuccessPayloadV1.Item item : payload.items()) {
            if (item.productId() != null && item.quantity() != null && item.quantity() > 0) {
                metricsService.addSales(item.productId(), item.quantity(), envelope.occurredAtEpochMillis());
                log.debug("Processed PAYMENT_SUCCESS for productId: {}, quantity: {}", 
                         item.productId(), item.quantity());
            }
        }
    }
}
