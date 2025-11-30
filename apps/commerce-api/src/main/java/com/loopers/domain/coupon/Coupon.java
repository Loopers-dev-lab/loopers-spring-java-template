package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Version
    private Long version;

    private Coupon(User user, String name, DiscountType discountType, Long discountValue) {
        validateFields(user, name, discountType, discountValue);
        this.user = user;
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.isUsed = false;
    }

    public static Coupon create(User user, String name, DiscountType discountType, Long discountValue) {
        return new Coupon(user, name, discountType, discountValue);
    }

    public Long calculateDiscount(Long originalAmount) {
        if (Boolean.TRUE.equals(this.isUsed)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }

        return switch (discountType) {
            case FIXED_AMOUNT -> Math.min(discountValue, originalAmount);
            case PERCENTAGE -> (originalAmount * discountValue) / 100;
        };
    }

    public void use() {
        if (Boolean.TRUE.equals(this.isUsed)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.isUsed = true;
    }

    public boolean canUse() {
        return !this.isUsed;
    }

    private void validateFields(User user, String name, DiscountType discountType, Long discountValue) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다.");
        }
        if (discountType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 타입은 필수입니다.");
        }
        if (discountValue == null || discountValue <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100% 이하여야 합니다.");
        }
    }
}
