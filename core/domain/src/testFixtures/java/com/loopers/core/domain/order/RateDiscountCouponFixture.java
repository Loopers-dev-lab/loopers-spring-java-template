package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.CouponDiscountRate;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class RateDiscountCouponFixture {

    public static RateDiscountCoupon create() {
        return Instancio.of(RateDiscountCoupon.class)
                .set(field(RateDiscountCoupon::getUserId), Instancio.create(UserId.class))
                .set(field(RateDiscountCoupon::getRate), new CouponDiscountRate(new BigDecimal(10)))
                .create();
    }

    public static RateDiscountCoupon createWith(CouponDiscountRate rate) {
        return Instancio.of(RateDiscountCoupon.class)
                .set(field(RateDiscountCoupon::getUserId), Instancio.create(UserId.class))
                .set(field(RateDiscountCoupon::getRate), rate)
                .create();
    }
}
