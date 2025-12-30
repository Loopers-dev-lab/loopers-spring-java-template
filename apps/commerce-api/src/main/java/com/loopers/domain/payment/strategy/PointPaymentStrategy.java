package com.loopers.domain.payment.strategy;

import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 포인트 결제 전략 구현
 * 포인트 차감을 통한 결제를 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointPaymentStrategy implements PaymentStrategy {
    
    private final PointService pointService;
    
    @Override
    public PaymentResult processPayment(Long orderId, Long userId, BigDecimal amount) {
        log.info("포인트 결제 처리 시작 - orderId: {}, userId: {}, amount: {}", 
                orderId, userId, amount);
        
        // 포인트 조회 및 검증
        var pointOpt = pointService.findByUserId(userId);
        if (pointOpt.isEmpty()) {
            log.error("포인트 정보를 찾을 수 없음 - userId: {}", userId);
            return new PaymentResult(
                false,
                null,
                PaymentDto.PaymentStatus.FAILED,
                "포인트 정보를 찾을 수 없습니다."
            );
        }
        
        var point = pointOpt.get();
        
        // 포인트 잔액 확인
        if (point.getAmount().compareTo(amount) < 0) {
            log.error("포인트 부족 - userId: {}, 현재 포인트: {}, 요청 금액: {}", 
                    userId, point.getAmount(), amount);
            return new PaymentResult(
                false,
                null,
                PaymentDto.PaymentStatus.FAILED,
                String.format("포인트가 부족합니다. (현재 포인트: %s, 요청 금액: %s)", 
                        point.getAmount(), amount)
            );
        }
        
        // 포인트 차감
        try {
            pointService.deduct(userId, amount);
            log.info("포인트 차감 성공 - userId: {}, 차감 금액: {}", userId, amount);
            
            // 포인트 결제는 즉시 성공 처리
            String transactionKey = "POINT_" + UUID.randomUUID().toString();
            
            return new PaymentResult(
                true,
                transactionKey,
                PaymentDto.PaymentStatus.SUCCESS,
                null
            );
        } catch (CoreException e) {
            log.error("포인트 차감 실패 - userId: {}, amount: {}, error: {}", 
                    userId, amount, e.getMessage());
            return new PaymentResult(
                false,
                null,
                PaymentDto.PaymentStatus.FAILED,
                e.getMessage()
            );
        }
    }
    
    @Override
    public PaymentDto.PaymentMethod getPaymentMethod() {
        return PaymentDto.PaymentMethod.POINT;
    }
}

