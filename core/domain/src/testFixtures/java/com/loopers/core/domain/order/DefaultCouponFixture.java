package com.loopers.core.domain.order;

import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class DefaultCouponFixture {

    public static DefaultCoupon create() {
        return Instancio.of(DefaultCoupon.class)
                .set(field(DefaultCoupon::getUserId), Instancio.create(UserId.class))
                .set(field(DefaultCoupon::getStatus), CouponStatus.AVAILABLE)
                .create();
    }

    public static DefaultCoupon createWith(CouponStatus status) {
        return Instancio.of(DefaultCoupon.class)
                .set(field(DefaultCoupon::getUserId), Instancio.create(UserId.class))
                .set(field(DefaultCoupon::getStatus), status)
                .create();
    }
}
