package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.CouponDiscountRate;
import com.loopers.core.domain.order.vo.RateDiscountCouponId;
import lombok.Getter;

@Getter
public class RateDiscountCoupon extends AbstractCoupon {

    private final RateDiscountCouponId id;

    private final CouponDiscountRate rate;

    private RateDiscountCoupon(
            AbstractCoupon abstractCoupon,
            RateDiscountCouponId id,
            CouponDiscountRate rate) {
        super(abstractCoupon);
        this.id = id;
        this.rate = rate;
    }

    public static RateDiscountCoupon create(AbstractCoupon abstractCoupon, CouponDiscountRate rate) {
        return new RateDiscountCoupon(abstractCoupon, RateDiscountCouponId.empty(), rate);
    }

    public static RateDiscountCoupon mappedBy(RateDiscountCouponId id, AbstractCoupon abstractCoupon, CouponDiscountRate rate) {
        return new RateDiscountCoupon(abstractCoupon, id, rate);
    }
}
