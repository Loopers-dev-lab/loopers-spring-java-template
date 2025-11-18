package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public abstract class AbstractCoupon implements Coupon {

    protected CouponId couponId;

    protected UserId userId;

    protected CouponStatus status;

    protected CreatedAt createdAt;

    protected UpdatedAt updatedAt;

    protected DeletedAt deletedAt;

    protected AbstractCoupon(
            CouponId couponId,
            UserId userId,
            CouponStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    protected AbstractCoupon(
            AbstractCoupon abstractCoupon
    ) {
        this.couponId = abstractCoupon.couponId;
        this.userId = abstractCoupon.userId;
        this.status = abstractCoupon.status;
        this.createdAt = abstractCoupon.createdAt;
        this.updatedAt = abstractCoupon.updatedAt;
        this.deletedAt = abstractCoupon.deletedAt;
    }

    @Override
    public void use() {
        this.status = CouponStatus.USED;
    }

    public PayAmount discount(PayAmount payAmount) {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new IllegalArgumentException(DomainErrorCode.NOT_AVAILABLE_COUPON_STATUS.getMessage());
        }

        BigDecimal discountAmount = calculateDiscountAmount(payAmount);
        return payAmount.minus(discountAmount);
    }

    public abstract BigDecimal calculateDiscountAmount(PayAmount payAmount);
}
