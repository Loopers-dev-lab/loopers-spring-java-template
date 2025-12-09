package com.loopers.core.infra.httpclient.dataplatform.order.impl;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDataPlatformClient;
import org.springframework.stereotype.Component;

@Component
public class OrderDataPlatformClientImpl implements OrderDataPlatformClient {

    @Override
    public void send(Order order) {
        
    }
}
