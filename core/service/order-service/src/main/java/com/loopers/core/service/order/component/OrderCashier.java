package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
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
    private final PaymentRepository paymentRepository;

    public Payment checkout(User user, Order order, PayAmount payAmount) {
        UserPoint userPoint = userPointRepository.getByUserId(user.getUserId());
        userPointRepository.save(userPoint.pay(payAmount));

        Payment payment = Payment.create(order.getOrderId(), user.getUserId(), payAmount);
        paymentRepository.save(payment);

        return payment;
    }
}
