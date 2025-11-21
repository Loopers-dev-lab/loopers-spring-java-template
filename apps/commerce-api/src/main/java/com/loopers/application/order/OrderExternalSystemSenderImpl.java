package com.loopers.application.order;

import com.loopers.domain.order.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderExternalSystemSenderImpl implements OrderExternalSystemSender{
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(Order order) {

    }
}
