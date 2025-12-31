package com.loopers.application.metrics;

import com.loopers.application.metrics.ProductMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsAggregator {

    private final ProductMetricsService productMetricsService;

    /**
     * 배치로 들어온 이벤트를 해석하여 지표를 반영한다.
     */
    @Transactional
    public void aggregate(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (Map<String, Object> event : events) {
            Object typeObj = event.get("eventType");
            Object payloadObj = event.get("payload");
            if (!(typeObj instanceof String) || !(payloadObj instanceof Map)) {
                continue;
            }
            String eventType = (String) typeObj;
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) payloadObj;

            switch (eventType) {
                case "LIKE_CREATED" -> handleLikeCreated(payload);
                case "LIKE_DELETED" -> handleLikeDeleted(payload);
                case "PRODUCT_VIEWED" -> handleProductViewed(payload);
                case "ORDER_ITEM_METRIC" -> handleOrderItemMetric(payload);
                default -> {
                }
            }
        }
    }

    private void handleLikeCreated(Map<String, Object> payload) {
        Long productId = asLong(payload.get("productId"));
        if (productId == null) return;
        productMetricsService.incrementLikeCount(productId);
    }

    private void handleLikeDeleted(Map<String, Object> payload) {
        Long productId = asLong(payload.get("productId"));
        if (productId == null) return;
        productMetricsService.decrementLikeCount(productId);
    }

    private void handleProductViewed(Map<String, Object> payload) {
        Long productId = asLong(payload.get("productId"));
        if (productId == null) return;
        productMetricsService.incrementViewCount(productId);
    }

    private void handleOrderItemMetric(Map<String, Object> payload) {
        Long productId = asLong(payload.get("productId"));
        Integer quantity = asInt(payload.get("quantity"));
        BigDecimal amount = asDecimal(payload.get("totalAmount"));
        if (productId == null || quantity == null || amount == null) return;
        productMetricsService.incrementOrderMetrics(productId, quantity, amount);
    }

    private static Long asLong(Object v) {
        try {
            return v == null ? null : Long.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object v) {
        try {
            return v == null ? null : Integer.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal asDecimal(Object v) {
        try {
            return v == null ? null : new BigDecimal(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}



