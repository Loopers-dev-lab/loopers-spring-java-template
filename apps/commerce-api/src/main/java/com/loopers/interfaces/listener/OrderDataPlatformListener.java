package com.loopers.interfaces.listener;

import com.loopers.application.order.OrderEvent;
import com.loopers.domain.dataplatform.DataPlatformClient;
import com.loopers.domain.dataplatform.OrderData;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderDataPlatformListener {

    private final OrderService orderService;
    private final DataPlatformClient dataPlatformClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderEvent.OrderCreated event) {
        log.info("OrderCreated 이벤트 수신 - 데이터 플랫폼 전송: orderKey={}", event.orderKey());

        Order order = orderService.getOrderByOrderKey(event.orderKey());

        OrderData orderData = new OrderData(
                order.getOrderKey(),
                order.getUserId(),
                event.loginId(),
                event.originalTotalPrice(),
                event.discountPrice(),
                event.originalTotalPrice() - event.discountPrice(),
                order.getOrderStatus().name(),
                order.getCouponId(),
                order.getCreatedAt()
        );

        dataPlatformClient.sendOrderData(orderData);
    }
}

