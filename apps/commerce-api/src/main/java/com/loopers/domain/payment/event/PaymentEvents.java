package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.payment.PaymentDto;

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
        CouponEvents.Processed originalEvent  // nullable: PG 콜백 경로에서는 null
    ) {}
    
    /**
     * 결제 처리 실패 이벤트
     */
    public record ProcessingFailed(
        Long orderId,
        String reason
    ) {}
}

