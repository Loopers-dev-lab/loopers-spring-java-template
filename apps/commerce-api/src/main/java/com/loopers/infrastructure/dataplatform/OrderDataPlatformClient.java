package com.loopers.infrastructure.dataplatform;

import com.loopers.domain.order.Order;
import com.loopers.domain.payment.Payment;

public interface OrderDataPlatformClient {

    void send(Order order, Payment payment);
}

