package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "tb_coupon")
@Getter
public class Coupon extends BaseEntity {
    private Long userId;
    @Enumerated(EnumType.STRING)
    private CouponType couponType;
    private Integer discountValue;
    private boolean used;

    protected Coupon() {
    }

    public static Coupon issuePercentage(Long userId, Integer discountPercent) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 1 이상이어야 합니다.");
        }
        if (discountPercent == null || discountPercent <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
        if (discountPercent > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.");
        }
        Coupon coupon = new Coupon();
        coupon.userId = userId;
        coupon.couponType = CouponType.PERCENTAGE;
        coupon.discountValue = discountPercent;
        coupon.used = false;
        return coupon;
    }

    public static Coupon issueFixed(Long userId, Integer discountAmount) {
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 1 이상이어야 합니다.");
        }
        if (discountAmount == null || discountAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 값은 0보다 커야 합니다.");
        }
        Coupon coupon = new Coupon();
        coupon.userId = userId;
        coupon.couponType = CouponType.FIXED;
        coupon.discountValue = discountAmount;
        coupon.used = false;
        return coupon;
    }

    /**
     * 쿠폰 할인 적용 (사용 처리 포함)
     * 기존 동작 유지 (하위 호환성)
     */
    public DiscountResult applyDiscount(Price originalPrice) {
        DiscountResult result = calculateDiscount(originalPrice);
        markAsUsed();
        return result;
    }

    /**
     * 쿠폰 할인 계산 (사용 처리 없이 계산만)
     * 이벤트 기반 처리에서 사용
     */
    public DiscountResult calculateDiscount(Price originalPrice) {
        if (this.used) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        Price discountAmount;
        Price finalPrice;
        switch (couponType) {
            case FIXED -> {
                discountAmount = new Price(Math.min(originalPrice.amount(), discountValue));
                finalPrice = originalPrice.deduct(discountAmount);
            }
            case PERCENTAGE -> {
                long calculatedDiscount = ((long) originalPrice.amount() * discountValue) / 100;
                discountAmount = new Price((int) calculatedDiscount);
                finalPrice = originalPrice.deduct(discountAmount);
            }
            default -> throw new CoreException(ErrorType.BAD_REQUEST, "알 수 없는 쿠폰 타입입니다.");
        }
        return new DiscountResult(this.getId(), originalPrice, discountAmount, finalPrice);
    }

    /**
     * 쿠폰 사용 처리
     * 이벤트 핸들러에서 호출
     */
    public void markAsUsed() {
        if (this.used) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.used = true;
    }
}
