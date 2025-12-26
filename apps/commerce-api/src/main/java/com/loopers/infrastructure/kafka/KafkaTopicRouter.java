package com.loopers.infrastructure.kafka;

import org.springframework.stereotype.Component;

/**
 * Kafka Topic Router
 * - 이벤트 타입에 따라 적절한 Kafka 토픽을 반환
 * - 토픽 네이밍 규칙: loopers.commerce.{event-name}-v1
 */
@Component
public class KafkaTopicRouter {

    private static final String TOPIC_PREFIX = "loopers.commerce.";
    private static final String TOPIC_VERSION = "-v1";

    /**
     * 이벤트 타입에 맞는 토픽 이름 반환
     *
     * @param eventType 이벤트 타입 (ORDER_PLACED, PRODUCT_LIKED 등)
     * @return Kafka 토픽 이름
     */
    public String getTopicName(String eventType) {
        return switch (eventType) {
            // Order Events
            case "ORDER_PLACED" -> TOPIC_PREFIX + "order-placed" + TOPIC_VERSION;
            case "ORDER_COMPLETED" -> TOPIC_PREFIX + "order-completed" + TOPIC_VERSION;

            // Payment Events
            case "PAYMENT_COMPLETED" -> TOPIC_PREFIX + "payment-completed" + TOPIC_VERSION;

            // Product Events
            case "PRODUCT_LIKED" -> TOPIC_PREFIX + "product-liked" + TOPIC_VERSION;
            case "PRODUCT_UNLIKED" -> TOPIC_PREFIX + "product-unliked" + TOPIC_VERSION;
            case "PRODUCT_VIEWED" -> TOPIC_PREFIX + "product-viewed" + TOPIC_VERSION;

            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }

}
