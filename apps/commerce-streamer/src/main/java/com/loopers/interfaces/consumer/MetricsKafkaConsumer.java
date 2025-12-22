package com.loopers.interfaces.consumer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.loopers.infrastructure.event.payloads.StockDepletedPayloadV1;

import jakarta.annotation.PreDestroy;
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    @KafkaListener(
            topics = {"catalog-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onCatalogEvents(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment ack) {

        log.debug("Processing {} catalog events", records.size());

        // 언제든지 변경 될 가능성 존재
        List<CompletableFuture<Void>> futures = records.stream()
                .map(record -> CompletableFuture.runAsync(() -> {
                    try {
                        processCatalogEvent(record);
                    } catch (Exception e) {
                        log.error("Failed to process catalog event: {}", record.value(), e);
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

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

        List<CompletableFuture<Void>> futures = records.stream()
                .map(record -> CompletableFuture.runAsync(() -> {
                    try {
                        processOrderEvent(record);
                    } catch (Exception e) {
                        log.error("Failed to process order event: {}", record.value(), e);
                    }
                }, executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        ack.acknowledge();
        log.debug("Acknowledged {} order events", records.size());
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    private void processCatalogEvent(ConsumerRecord<Object, Object> record) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(record.value());
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", record.value());
            return;
        }

        // 과거 이벤트 필터링 (1시간 이상 된 이벤트는 무시)
        if (isOldEvent(envelope.occurredAtEpochMillis())) {
            log.debug("Ignoring old event: eventId={}, occurredAt={}",
                    envelope.eventId(), envelope.occurredAtEpochMillis());
            // 멱등성 테이블에는 기록하되 비즈니스 로직은 처리하지 않음
            metricsService.tryMarkHandled(envelope.eventId());
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
            case "STOCK_DEPLETED" -> handleStockDepleted(envelope);
            default -> log.debug("Unhandled catalog event type: {}", envelope.eventType());
        }
    }

    private void processOrderEvent(ConsumerRecord<Object, Object> record) {
        final DomainEventEnvelope envelope = eventDeserializer.deserializeEnvelope(record.value());
        if (envelope == null || envelope.eventId() == null) {
            log.warn("Invalid event envelope: {}", record.value());
            return;
        }

        // 과거 이벤트 필터링
        if (isOldEvent(envelope.occurredAtEpochMillis())) {
            log.debug("Ignoring old event: eventId={}, occurredAt={}",
                    envelope.eventId(), envelope.occurredAtEpochMillis());
            metricsService.tryMarkHandled(envelope.eventId());
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
        if (payload == null) {
            log.warn("Invalid PaymentSuccess payload: {}", envelope.payloadJson());
            return;
        }

        // 새로운 구조: 상품별 개별 이벤트 처리
        if (payload.productId() != null && payload.quantity() != null && payload.quantity() > 0) {
            metricsService.addSales(payload.productId(), payload.quantity(), envelope.occurredAtEpochMillis());

            log.debug(
                    "Processed PAYMENT_SUCCESS - orderId: {}, orderNumber: {}, userId: {}, productId: {}, quantity: {}, unitPrice: {}, totalPrice: {}",
                    payload.orderId(), payload.orderNumber(), payload.userId(),
                    payload.productId(), payload.quantity(), payload.unitPrice(), payload.totalPrice());
        } else {
            log.warn("Invalid PaymentSuccess payload - missing required fields: productId={}, quantity={}",
                    payload.productId(), payload.quantity());
        }
    }

    private void handleStockDepleted(DomainEventEnvelope envelope) {
        final StockDepletedPayloadV1 payload = eventDeserializer.deserializeStockDepleted(envelope.payloadJson());
        if (payload == null || payload.productId() == null) {
            log.warn("Invalid StockDepleted payload: {}", envelope.payloadJson());
            return;
        }

        // 재고 소진 이벤트 처리 - remainingStock 정보 전달
        metricsService.handleStockDepleted(payload.productId(), payload.brandId(), payload.remainingStock(),
                envelope.occurredAtEpochMillis());

        log.info("Processed STOCK_DEPLETED - productId: {}, brandId: {}, productName: {}, remainingStock: {}",
                payload.productId(), payload.brandId(), payload.productName(), payload.remainingStock());
    }

    /**
     * 과거 이벤트인지 확인 (1시간 이상 된 이벤트는 과거 이벤트로 간주)
     */
    private boolean isOldEvent(long occurredAtEpochMillis) {
        long currentTime = System.currentTimeMillis();
        long eventAge = currentTime - occurredAtEpochMillis;
        long oneHourInMillis = 60 * 60 * 1000; // 1시간

        return eventAge > oneHourInMillis;
    }
}
