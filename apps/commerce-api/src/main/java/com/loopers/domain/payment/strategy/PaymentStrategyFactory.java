package com.loopers.domain.payment.strategy;

import com.loopers.domain.payment.PaymentDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 결제 전략 팩토리
 * 결제 방법에 따라 적절한 전략을 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class PaymentStrategyFactory {
    
    private final List<PaymentStrategy> strategies;
    private Map<PaymentDto.PaymentMethod, PaymentStrategy> strategyMap;
    
    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
            .collect(Collectors.toMap(
                PaymentStrategy::getPaymentMethod,
                Function.identity()
            ));
    }
    
    /**
     * 결제 방법에 해당하는 전략을 반환합니다.
     * @param paymentMethod 결제 방법
     * @return 결제 전략
     * @throws CoreException 지원하지 않는 결제 방법인 경우
     */
    public PaymentStrategy getStrategy(PaymentDto.PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new CoreException(
                ErrorType.BAD_REQUEST,
                "결제 방법이 지정되지 않았습니다."
            );
        }
        
        PaymentStrategy strategy = strategyMap.get(paymentMethod);
        if (strategy == null) {
            throw new CoreException(
                ErrorType.BAD_REQUEST,
                "지원하지 않는 결제 방법입니다: " + paymentMethod
            );
        }
        
        return strategy;
    }
}

