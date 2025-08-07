package com.loopers.domain.coupon.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public class CouponIssued {

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    public CouponIssued() {
    }

    private CouponIssued(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public static CouponIssued of(LocalDateTime issuedAt) {
        LocalDateTime now = LocalDateTime.now();

        if (issuedAt == null) {
            issuedAt = now;
        } else if (issuedAt.isBefore(now.minusSeconds(1))) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급일은 현재 이후여야 합니다.");
        }

        return new CouponIssued(issuedAt);
    }

    public LocalDateTime getValue() {
        return this.issuedAt;
    }

}
