package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCashier {

    private final UserPointRepository userPointRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public Order checkout(User user, Order order, PayAmount payAmount) {
        UserPoint userPoint = userPointRepository.getByUserId(user.getUserId());
        userPointRepository.save(userPoint.pay(payAmount));

        Order savedOrder = orderRepository.save(order);

        Payment payment = Payment.create(savedOrder.getOrderId(), user.getUserId(), payAmount);
        paymentRepository.save(payment);

        return savedOrder;
    }
}
