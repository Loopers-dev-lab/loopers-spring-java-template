package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;
import com.loopers.domain.point.PointAccountDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointPaymentStrategy implements PaymentStrategy {

    private final PointAccountDomainService pointAccountDomainService;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.POINT_ONLY;
    }

    @Override
    public void executePayment(PaymentContext context) {
        pointAccountDomainService.deduct(context.userId(), context.pointAmount());
    }
}
