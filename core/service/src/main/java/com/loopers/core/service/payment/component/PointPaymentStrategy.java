package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentType;
import com.loopers.core.domain.payment.vo.PaymentCompletedEvent;
import com.loopers.core.domain.payment.vo.PaymentCreatedEvent;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PointPaymentStrategy implements PaymentStrategy {

    private final UserPointRepository userPointRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.POINT;
    }

    @Override
    @Transactional
    public void pay(Payment payment) {
        UserPoint userPoint = userPointRepository.getByUserId(payment.getUserId());
        userPointRepository.save(userPoint.pay(payment.getAmount()));
        eventPublisher.publishEvent(new PaymentCompletedEvent(payment.getId()));
        eventPublisher.publishEvent(new PaymentCreatedEvent(payment.getId(), payment.getType()));
    }
}
