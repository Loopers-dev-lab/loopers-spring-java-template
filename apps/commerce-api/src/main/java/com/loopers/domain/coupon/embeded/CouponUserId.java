package com.loopers.domain.coupon.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CouponUserId {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    public CouponUserId() {

    }

    private CouponUserId(Long userId) {
        this.userId = userId;
    }

    public static CouponUserId of(Long userId) {
        if(userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "userId cannot be null");
        }
        return new CouponUserId(userId);
    }

    public Long getValue() {
        return this.userId;
    }
}
