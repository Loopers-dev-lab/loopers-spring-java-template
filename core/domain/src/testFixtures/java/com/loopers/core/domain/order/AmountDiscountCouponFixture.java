package com.loopers.core.domain.order;

import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.AmountDiscountCouponId;
import com.loopers.core.domain.order.vo.CouponDiscountAmount;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class AmountDiscountCouponFixture {

    public static AmountDiscountCoupon create() {
        return Instancio.of(AmountDiscountCoupon.class)
                .set(field(AmountDiscountCoupon::getCouponId), CouponId.empty())
                .set(field(AmountDiscountCoupon::getId), AmountDiscountCouponId.empty())
                .set(field(AmountDiscountCoupon::getUserId), new UserId("1"))
                .set(field(AmountDiscountCoupon::getAmount), new CouponDiscountAmount(new BigDecimal(1000)))
                .create();
    }

    public static AmountDiscountCoupon createWith(CouponDiscountAmount couponDiscountAmount) {
        return Instancio.of(AmountDiscountCoupon.class)
                .set(field(AmountDiscountCoupon::getCouponId), CouponId.empty())
                .set(field(AmountDiscountCoupon::getId), AmountDiscountCouponId.empty())
                .set(field(AmountDiscountCoupon::getUserId), new UserId("1"))
                .set(field(AmountDiscountCoupon::getAmount), couponDiscountAmount)
                .create();
    }

    public static AmountDiscountCoupon createWith(CouponStatus status) {
        return Instancio.of(AmountDiscountCoupon.class)
                .set(field(AmountDiscountCoupon::getCouponId), CouponId.empty())
                .set(field(AmountDiscountCoupon::getId), AmountDiscountCouponId.empty())
                .set(field(AmountDiscountCoupon::getUserId), new UserId("1"))
                .set(field(AmountDiscountCoupon::getAmount), new CouponDiscountAmount(new BigDecimal(1000)))
                .set(field(AmountDiscountCoupon::getStatus), status)
                .create();
    }
}
