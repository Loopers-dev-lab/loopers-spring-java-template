package com.loopers.core.infra.httpclient.dataplatform.payment.impl;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDataPlatformClient;
import org.springframework.stereotype.Component;

@Component
public class PaymentDatabasePlatformClientImpl implements OrderDataPlatformClient {

    @Override
    public void send(Order order) {

    }
}
