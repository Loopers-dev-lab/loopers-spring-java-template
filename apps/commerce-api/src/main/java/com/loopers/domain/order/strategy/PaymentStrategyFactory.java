package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentType;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {

    private final List<PaymentStrategy> strategies;

    public PaymentStrategy create(PaymentType paymentType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(paymentType))
                .findFirst()
                .orElseThrow(() -> new CoreException(
                        ErrorType.BAD_REQUEST,
                        "지원하지 않는 결제 방식입니다: " + paymentType
                ));
    }
}