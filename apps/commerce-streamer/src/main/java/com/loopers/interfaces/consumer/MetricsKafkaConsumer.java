package com.loopers.interfaces.consumer;

import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.metrics.MetricsService;

import lombok.RequiredArgsConstructor;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */

@Component
@RequiredArgsConstructor
public class MetricsKafkaConsumer {

    private final MetricsService metricsService;

    @KafkaListener(
            topics = {"catalog-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onCatalogEvents(
            List<ConsumerRecord<Object, Object>> records,
            Acknowledgment ack) {
        for (ConsumerRecord<Object, Object> record : records) {
            final Map<String, Object> event = asMap(record.value());
            final String eventId = str(event.get("eventId"));
            if (eventId == null) {
                continue;
            }

            final boolean first = metricsService.tryMarkHandled(eventId);
            if (!first) {
                continue;
            }

            final String eventType = str(event.get("eventType"));
            final Map<String, Object> payload = asMap(event.get("payload"));

            if ("PRODUCT_VIEW".equals(eventType)) {
                final Long productId = longVal(payload.get("productId"));
                if (productId != null) {
                    metricsService.incrementView(productId);
                }
            }

            if ("LIKE_ACTION".equals(eventType)) {
                final Long productId = longVal(payload.get("productId"));
                final String action = str(payload.get("action")); // LIKE / UNLIKE
                if (productId != null && action != null) {
                    final int delta = "LIKE".equals(action) ? 1 : -1;
                    metricsService.applyLikeDelta(productId, delta);
                }
            }
        }

        ack.acknowledge();
    }

    @KafkaListener(
            topics = {"order-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void onOrderEvents(
            final List<ConsumerRecord<Object, Object>> records,
            final Acknowledgment ack
    ) {
        for (ConsumerRecord<Object, Object> record : records) {
            final Map<String, Object> event = asMap(record.value());
            final String eventId = str(event.get("eventId"));
            if (eventId == null) {
                continue;
            }

            final boolean first = metricsService.tryMarkHandled(eventId);
            if (!first) {
                continue;
            }

            final String eventType = str(event.get("eventType"));

            //
            if (!"PAYMENT_SUCCESS".equals(eventType)) {
                continue;
            }

            // 기대 payload:
            // { "items": [ { "productId": 1, "quantity": 2 }, ... ] }
            final Map<String, Object> payload = asMap(event.get("payload"));
            final List<Map<String, Object>> items = listOfMap(payload.get("items"));
            for (Map<String, Object> item : items) {
                final Long productId = longVal(item.get("productId"));
                final Integer quantity = intVal(item.get("quantity"));
                if (productId != null && quantity != null) {
                    metricsService.addSales(productId, quantity);
                }
            }
        }

        ack.acknowledge();
    }

    private Map<String, Object> asMap(final Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private List<Map<String, Object>> listOfMap(final Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(it -> it instanceof Map<?, ?>)
                    .map(it -> (Map<String, Object>) it)
                    .toList();
        }
        return List.of();
    }

    private String str(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(final Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer intVal(final Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }


}
