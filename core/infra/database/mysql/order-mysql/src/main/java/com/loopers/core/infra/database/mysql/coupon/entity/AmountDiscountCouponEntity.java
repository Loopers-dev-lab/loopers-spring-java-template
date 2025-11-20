package com.loopers.core.infra.database.mysql.coupon.entity;

import com.loopers.core.domain.order.AmountDiscountCoupon;
import com.loopers.core.domain.order.vo.AmountDiscountCouponId;
import com.loopers.core.domain.order.vo.CouponDiscountAmount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Entity
@Table(
        name = "amount_discount_coupon",
        indexes = {
                @Index(name = "idx_amount_discount_coupon_coupon_id", columnList = "coupon_id"),
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AmountDiscountCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public static AmountDiscountCouponEntity of(CouponEntity couponEntity, AmountDiscountCoupon amountDiscountCoupon) {
        return new AmountDiscountCouponEntity(
                Optional.ofNullable(amountDiscountCoupon.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                couponEntity.getId(),
                amountDiscountCoupon.getAmount().value()
        );
    }

    public AmountDiscountCoupon to(CouponEntity couponEntity) {
        return AmountDiscountCoupon.mappedBy(
                new AmountDiscountCouponId(this.id.toString()),
                couponEntity.to(),
                new CouponDiscountAmount(this.amount)
        );
    }
}
