package com.loopers.domain.payment.strategy;

import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PgPaymentGateway;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PG 결제 전략 구현
 * 카드 결제를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgPaymentStrategy implements PaymentStrategy {
    
    private final PgPaymentGateway pgPaymentGateway;
    
    @Value("${pg.api.callbackUrl}")
    private String pgCallbackUrl;
    
    @Override
    public PaymentResult processPayment(Long orderId, Long userId, BigDecimal amount) {
        log.info("PG 결제 처리 시작 - orderId: {}, amount: {}", orderId, amount);
        
        ApiResponse<PaymentDto.PgResponse> pgApiResponse = pgPaymentGateway.approvePayment(
            userId,
            PaymentDto.PgRequest.builder()
                .orderId(String.format("%06d", orderId))  // 6자리 문자열로 변환
                .cardNo("1111-2222-3333-4444")
                .cardType(PaymentDto.CardType.SAMSUNG)
                .amount(amount.longValue())
                .callbackUrl(pgCallbackUrl)
                .build()
        );
        
        PaymentDto.PgResponse pgResponse = pgApiResponse.data();
        
        if (pgResponse.status() == PaymentDto.PaymentStatus.PENDING) {
            log.info("PG 결제 요청 성공 (대기 중) - orderId: {}, transactionKey: {}", 
                    orderId, pgResponse.transactionKey());
            return new PaymentResult(
                true,
                pgResponse.transactionKey(),
                PaymentDto.PaymentStatus.PENDING,
                null
            );
        } else {
            log.error("PG 결제 요청 실패 - orderId: {}, reason: {}", 
                    orderId, pgResponse.reason());
            return new PaymentResult(
                false,
                pgResponse.transactionKey(),
                pgResponse.status(),
                pgResponse.reason()
            );
        }
    }
    
    @Override
    public PaymentDto.PaymentMethod getPaymentMethod() {
        return PaymentDto.PaymentMethod.CARD;
    }
}

