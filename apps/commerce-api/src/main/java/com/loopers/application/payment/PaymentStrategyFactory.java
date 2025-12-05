package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentMethod;
import com.loopers.domain.payment.PaymentStrategy;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.loopers.domain.payment.PaymentMethod.PG_CARD;
import static com.loopers.domain.payment.PaymentMethod.POINT;

@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final PointPaymentStrategy pointPaymentStrategy;
    private final CardPaymentStrategy cardPaymentStrategy;

    public PaymentStrategy getStrategy(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case POINT -> pointPaymentStrategy;
            case PG_CARD -> cardPaymentStrategy;
            default -> throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "지원하지 않는 결제 방식입니다: " + paymentMethod
            );
        };
    }
}
