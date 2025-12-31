package com.loopers.application.payment;

import com.loopers.application.payment.event.PaymentFailedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.messaging.event.OrderPaidEvent;
import com.loopers.messaging.event.OrderPaidEvent.OrderItemData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessService {

    private final PaymentFacade paymentFacade;
    private final OrderService orderService; // Added OrderService injection

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void process(Long userId, Long orderId) {
        try {
            paymentFacade.pay(userId, orderId);
        } catch (Exception e) {
            log.error("결제 처리 중 오류 발생, orderId={}", orderId, e);
            throw e;
        }
    }

    @Transactional
    public void processPg(Long userId, Long orderId) { // Added userId parameter
        try {
            paymentFacade.payPg(orderId);

            Order order = orderService.findOrderById(orderId)
                    .orElseThrow(() -> new IllegalStateException("결제된 주문 정보를 찾을 수 없습니다. orderId: " + orderId));

            List<OrderItemData> orderItemDataList = order.getItems().stream()
                    .map(item -> new OrderItemData(item.getProductId(), item.getQuantity(), item.getUnitPrice().getAmount()))
                    .collect(Collectors.toList());

            eventPublisher.publishEvent(OrderPaidEvent.of(orderId, orderItemDataList));
        } catch (Exception e) {
            log.error("외부 PG 결제 실패, 주문 ID: {}", orderId, e);
            eventPublisher.publishEvent(PaymentFailedEvent.of(userId, orderId, e));
        }
    }
}
