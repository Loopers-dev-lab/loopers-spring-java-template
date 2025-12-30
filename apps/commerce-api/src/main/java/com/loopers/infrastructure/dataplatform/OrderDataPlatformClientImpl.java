package com.loopers.infrastructure.dataplatform;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.outbox.OutboxService;
import com.loopers.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderDataPlatformClientImpl implements OrderDataPlatformClient {

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";

    private final OutboxService outboxService;

    @Override
    public void send(Order order, Payment payment) {
        if (order == null || payment == null) {
            return;
        }

        ZonedDateTime occurredAt = ZonedDateTime.now();
        String eventId = outboxService.nextEventId();
        Map<String, Object> payload = buildPayload(eventId, order, payment, occurredAt);
        outboxService.append(
                eventId,
                ORDER_EVENTS_TOPIC,
                order.getId().toString(),
                ORDER_STATUS_CHANGED,
                payload,
                occurredAt
        );
    }

    private Map<String, Object> buildPayload(
            String eventId,
            Order order,
            Payment payment,
            ZonedDateTime occurredAt
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", eventId);
        payload.put("eventType", ORDER_STATUS_CHANGED);
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("orderStatus", order.getStatus());
        payload.put("paymentStatus", payment.getStatus());
        payload.put("transactionKey", payment.getTransactionKey());
        payload.put("reason", payment.getReason());
        payload.put("occurredAt", occurredAt);
        payload.put("items", toItemPayload(order.getOrderItems()));
        return payload;
    }

    private List<Map<String, Object>> toItemPayload(List<OrderItem> items) {
        return items.stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", item.getProductId());
                    row.put("productName", item.getProductName());
                    row.put("quantity", item.getQuantity());
                    row.put("price", item.getPrice());
                    return row;
                })
                .collect(Collectors.toList());
    }
}
