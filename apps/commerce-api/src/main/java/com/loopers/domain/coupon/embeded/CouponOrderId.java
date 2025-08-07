package com.loopers.domain.coupon.embeded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CouponOrderId {
    
    @Column(name = "order_id")
    private Long orderId;

    public CouponOrderId() {
    }

    private CouponOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public static CouponOrderId of(Long orderId) {
        return new CouponOrderId(orderId);
    }

    public Long getValue() {
        return this.orderId;
    }

    public boolean isUsed() {
        return this.orderId != null;
    }

}
