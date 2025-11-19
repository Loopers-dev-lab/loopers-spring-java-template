package com.loopers.core.domain.order;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.vo.UserId;
import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;

public class DefaultCoupon extends AbstractCoupon {

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private DefaultCoupon(
            CouponId couponId,
            UserId userId,
            CouponStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt,
            Long version
    ) {
        super(couponId, userId, status, createdAt, updatedAt, deletedAt, version);
    }

    protected DefaultCoupon(AbstractCoupon coupon) {
        super(coupon);
    }

    public static DefaultCoupon create(UserId userId) {
        return new DefaultCoupon(
                CouponId.empty(),
                userId,
                CouponStatus.AVAILABLE,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty(),
                0L
        );
    }

    public static DefaultCoupon mappedBy(
            CouponId couponId,
            UserId userId,
            CouponStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt,
            Long version
    ) {
        return new DefaultCoupon(couponId, userId, status, createdAt, updatedAt, deletedAt, version);
    }

    @Override
    public BigDecimal calculateDiscountAmount(PayAmount payAmount) {
        return BigDecimal.ZERO;
    }
}
