package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_coupon_user_id", columnList = "userId"),
                @Index(name = "idx_coupon_status", columnList = "status")
        }
)
public class Coupon extends BaseEntity {

    private String userId;

    private String name;

    private long discountAmount;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private LocalDateTime usedAt;

    protected Coupon() {
    }

    private Coupon(String userId, String name, long discountAmount) {
        validateUserId(userId);
        validateName(name);
        validateDiscountAmount(discountAmount);
        this.userId = userId;
        this.name = name;
        this.discountAmount = discountAmount;
        this.status = CouponStatus.ISSUED;
    }

    public static Coupon create(String userId, String name, long discountAmount) {
        return new Coupon(userId, name, discountAmount);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 필수입니다.");
        }
    }

    private void validateDiscountAmount(long discountAmount) {
        if (discountAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0보다 커야 합니다.");
        }
    }

    public void use() {
        if (this.status == CouponStatus.USED) {
            log.warn("Coupon is already USED. Idempotent operation. couponId={}", this.getId());
            return;
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public boolean isUsable() {
        return this.status == CouponStatus.ISSUED;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public long getDiscountAmount() {
        return discountAmount;
    }

    public CouponStatus getStatus() {
        return status;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
