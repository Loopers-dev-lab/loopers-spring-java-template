package com.loopers.core.domain.order;

import com.loopers.core.domain.order.vo.AmountDiscountCouponId;
import com.loopers.core.domain.order.vo.CouponDiscountAmount;
import com.loopers.core.domain.payment.vo.PayAmount;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AmountDiscountCoupon extends AbstractCoupon {

    private final AmountDiscountCouponId id;

    private final CouponDiscountAmount amount;

    private AmountDiscountCoupon(AbstractCoupon abstractCoupon, AmountDiscountCouponId id, CouponDiscountAmount amount) {
        super(abstractCoupon);
        this.id = id;
        this.amount = amount;
    }

    public static AmountDiscountCoupon create(AbstractCoupon abstractCoupon, CouponDiscountAmount amount) {
        return new AmountDiscountCoupon(abstractCoupon, AmountDiscountCouponId.empty(), amount);
    }

    public static AmountDiscountCoupon mappedBy(AmountDiscountCouponId id, AbstractCoupon abstractCoupon, CouponDiscountAmount amount) {
        return new AmountDiscountCoupon(abstractCoupon, id, amount);
    }


    @Override
    public BigDecimal calculateDiscountAmount(PayAmount payAmount) {
        return this.amount.value();
    }
}
