package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrderCashier {

    private final UserPointRepository userPointRepository;
    private final PaymentRepository paymentRepository;
    private final PayAmountDiscountStrategySelector discountStrategySelector;

    @Transactional
    public Payment checkout(User user, Order order, PayAmount payAmount, CouponId couponId) {
        UserPoint userPoint = userPointRepository.getByUserIdWithLock(user.getId());
        userPointRepository.save(userPoint.pay(payAmount));
        PayAmountDiscountStrategy discountStrategy = discountStrategySelector.select(couponId);
        PayAmount discountedPayAmount = discountStrategy.discount(payAmount, couponId);
        Payment payment = Payment.create(order.getId(), user.getId(), discountedPayAmount);

        return paymentRepository.save(payment);
    }
}
