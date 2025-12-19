package com.loopers.interfaces.listener;

import com.loopers.application.order.OrderEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderEventListener {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderEvent.OrderCreated event) {
        log.info("OrderCreated 이벤트 수신 - 결제 생성 및 재고 차감: orderKey={}", event.orderKey());
        
        paymentService.createPayment(event.originalTotalPrice() - event.discountPrice(), event.orderKey());
        
        Order order = orderService.getOrderByOrderKey(event.orderKey());
        for (OrderItem orderItem : order.getOrderItems()) {
            productService.decreaseStock(orderItem.getProductId(), orderItem.getQuantity());
        }
    }

}

