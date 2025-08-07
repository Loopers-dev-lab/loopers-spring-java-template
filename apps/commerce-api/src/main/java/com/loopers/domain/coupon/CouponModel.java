package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.embeded.*;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
public class CouponModel extends BaseEntity {

    @Embedded
    private CouponUserId userId;
    @Embedded
    private CouponType type;
    @Embedded
    private CouponValue value;
    @Embedded
    private CouponUsed used;
    @Embedded
    private CouponOrderId orderId;
    @Embedded
    private CouponIssued issuedAt;
    @Embedded
    private CouponExpiration expiredAt;

    public CouponModel() {
    }

    private CouponModel(CouponUserId userId, CouponType type, CouponValue value, 
                       CouponUsed used, CouponOrderId orderId, 
                       CouponIssued issuedAt, CouponExpiration expiredAt) {
        this.userId = userId;
        this.type = type;
        this.value = value;
        this.used = used;
        this.orderId = orderId;
        this.issuedAt = issuedAt;
        this.expiredAt = expiredAt;
    }

    public static CouponModel createFixed(Long userId, BigDecimal amount, LocalDateTime expiredAt) {
        return createFixedWithIssueDate(userId, amount, LocalDateTime.now(), expiredAt);
    }

    public static CouponModel createRate(Long userId, BigDecimal rate, LocalDateTime expiredAt) {
        return createRateWithIssueDate(userId, rate, LocalDateTime.now(), expiredAt);
    }

    public static CouponModel createFixedWithIssueDate(Long userId, BigDecimal amount, 
                                                       LocalDateTime issuedAt, LocalDateTime expiredAt) {
        return new CouponModel(
                CouponUserId.of(userId),
                CouponType.of("FIXED"),
                CouponValue.fixed(amount),
                CouponUsed.of(false),
                CouponOrderId.of(null),
                CouponIssued.of(issuedAt),
                CouponExpiration.of(expiredAt)
        );
    }

    public static CouponModel createRateWithIssueDate(Long userId, BigDecimal rate, 
                                                     LocalDateTime issuedAt, LocalDateTime expiredAt) {
        return new CouponModel(
                CouponUserId.of(userId),
                CouponType.of("RATE"),
                CouponValue.rate(rate),
                CouponUsed.of(false),
                CouponOrderId.of(null),
                CouponIssued.of(issuedAt),
                CouponExpiration.of(expiredAt)
        );
    }

    public boolean canUse() {
        return !used.isUsed() && !isExpired();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt.getValue());
    }

    public void use(Long orderId) {
        if (this.used.isUsed()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        
        if (!canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }
        
        this.used = this.used.markUsed();
        this.orderId = CouponOrderId.of(orderId);
    }


    public BigDecimal calculateDiscountAmount(BigDecimal originalAmount) {
        if (!canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }

        if (type.getValue() == CouponType.CouponTypeEnum.FIXED) {
            return originalAmount.min(value.getValue());
        } else {
            return originalAmount.multiply(value.getValue())
                    .setScale(0, RoundingMode.DOWN);
        }
    }

    public boolean belongsToUser(Long userId) {
        return this.userId.getValue().equals(userId);
    }

    public boolean isUsed() {
        return this.used.isUsed();
    }

}
