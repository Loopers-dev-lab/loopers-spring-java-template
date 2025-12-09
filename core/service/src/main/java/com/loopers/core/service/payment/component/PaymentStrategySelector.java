package com.loopers.core.service.payment.component;

import com.loopers.core.domain.payment.type.PaymentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentStrategySelector {

    private final Map<PaymentType, PaymentStrategy> paymentStrategyMap;

    public PaymentStrategySelector(List<PaymentStrategy> paymentStrategies) {
        this.paymentStrategyMap = paymentStrategies.stream()
                .collect(Collectors.toMap(PaymentStrategy::getPaymentType, paymentStrategy -> paymentStrategy));
    }

    public PaymentStrategy selectBy(PaymentType paymentType) {
        return paymentStrategyMap.get(paymentType);
    }
}
