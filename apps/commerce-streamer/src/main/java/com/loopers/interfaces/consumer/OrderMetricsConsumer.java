package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.event.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import com.loopers.domain.product.ProductCacheRefreshService;
import com.loopers.ranking.RankingService;
import com.loopers.interfaces.consumer.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMetricsConsumer {

    private static final String TOPIC = "order-events";
    private static final String HANDLER = "order-metrics-consumer";

    private final ProductMetricsService productMetricsService;
    private final ProductCacheRefreshService productCacheRefreshService;
    private final EventHandledService eventHandledService;
    private final RankingService rankingService;

    @KafkaListener(
            topics = TOPIC,
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(
            List<OrderEventMessage> messages,
            Acknowledgment acknowledgment
    ) {
        if (messages == null || messages.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        try {
            for (OrderEventMessage message : messages) {
                if (!isValid(message)) {
                    continue;
                }
                if (eventHandledService.isHandled(message.eventId())) {
                    log.debug("Skip already handled order event eventId={}", message.eventId());
                    continue;
                }
                long version = extractVersion(message);
                if (shouldCountSale(message)) {
                    handleSalesMetrics(message, version);
                }
                eventHandledService.markHandled(
                        message.eventId(),
                        HANDLER,
                        TOPIC,
                        message.eventType(),
                        message.occurredAt()
                );
            }
            acknowledgment.acknowledge();
        } catch (Exception exception) {
            log.error("Failed to process order events", exception);
            throw exception;
        }
    }

    private boolean isValid(OrderEventMessage message) {
        return message != null
                && message.eventId() != null
                && message.items() != null;
    }

    private boolean shouldCountSale(OrderEventMessage message) {
        return Objects.equals(message.orderStatus(), "COMPLETE");
    }

    private void handleSalesMetrics(OrderEventMessage message, long version) {
        for (OrderEventMessage.OrderItemPayload item : message.items()) {
            if (item == null || item.productId() == null || item.quantity() == null) {
                continue;
            }
            productMetricsService.increaseSales(item.productId(), item.quantity(), version);
            productCacheRefreshService.refreshIfSoldOut(item.productId());
            rankingService.recordOrderEvent(item.productId(), item.price(), item.quantity(), message.occurredAt());
        }
    }

    private long extractVersion(OrderEventMessage message) {
        return message.occurredAt() == null
                ? System.currentTimeMillis()
                : message.occurredAt().toInstant().toEpochMilli();
    }
}
