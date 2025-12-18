package com.loopers.application.order;

import com.loopers.domain.order.Order;
import java.util.List;

public record OrderInfo(Long id, Long userId, List<OrderItemInfo> orderItemInfos, Long paymentId) {
    public static OrderInfo from(final Order order, final List<OrderItemInfo> orderItemInfos, final Long paymentId) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                orderItemInfos,
                paymentId
        );
    }
}
