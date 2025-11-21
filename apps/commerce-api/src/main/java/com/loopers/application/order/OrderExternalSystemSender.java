package com.loopers.application.order;

import com.loopers.domain.order.Order;

public interface OrderExternalSystemSender {
    void send(Order order);
}
