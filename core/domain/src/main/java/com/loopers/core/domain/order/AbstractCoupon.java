package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

@Getter
public abstract class AbstractCoupon implements Coupon {

    protected final CouponId couponId;

    protected final UserId userId;

    protected final CouponStatus status;

    protected final CreatedAt createdAt;

    protected final UpdatedAt updatedAt;

    protected final DeletedAt deletedAt;

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
}
