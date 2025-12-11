package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.payment.PaymentDto;

import java.math.BigDecimal;

public class PaymentEvents {
    
    /**
     * PG 콜백 수신 이벤트
     * PaymentFacade에서 PG 콜백을 받아 발행
     */
    public record CallbackReceived(
        String transactionKey,
        Long orderId,
        PaymentDto.PaymentStatus status,
        String reason
    ) {}
    
    /**
     * 결제 처리 완료 이벤트
     */
    public record Processed(
        Long orderId,
        Long userId,  // 결제 처리를 위해 필요
        BigDecimal finalAmount,  // 최종 결제 금액
        CouponEvents.Processed originalEvent  // nullable: PG 콜백 경로에서는 null
    ) {}
    
    /**
     * 결제 처리 실패 이벤트
     */
    public record ProcessingFailed(
        Long orderId,
        CouponEvents.Processed originalEvent,  // 재고 원복을 위해 필요 (nullable)
        String reason
    ) {}
}

