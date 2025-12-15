package com.loopers.application.order;

public interface OrderExternalSystemSender {
    void send(Long orderId);
}
