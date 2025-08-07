package com.loopers.domain.coupon.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Embeddable
public class CouponExpiration {
    private LocalDateTime expiredAt;

    public CouponExpiration() {
    }

    private CouponExpiration(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public LocalDateTime getValue() {
        return expiredAt;
    }

    public static CouponExpiration of(LocalDateTime expiredAt) {
        if (expiredAt == null || expiredAt.isBefore(LocalDate.now().atStartOfDay())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "만료 기안은 현재 날짜보다 길어야합니다.");
        }
        return new CouponExpiration(expiredAt);
    }
}
