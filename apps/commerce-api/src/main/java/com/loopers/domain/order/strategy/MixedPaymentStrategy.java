package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MixedPaymentStrategy implements PaymentStrategy {

    private final PointAccountDomainService pointAccountDomainService;

    @Override
    public boolean supports(PaymentType paymentType) {
        return paymentType == PaymentType.MIXED;
    }

    @Override
    public void executePayment(PaymentContext context) {
        if (context.pointAmount() > 0) {
            pointAccountDomainService.deduct(context.userId(), context.pointAmount());
        }

        if (context.cardAmount() > 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "카드 결제는 아직 지원하지 않습니다."
            );
        }
    }
}
