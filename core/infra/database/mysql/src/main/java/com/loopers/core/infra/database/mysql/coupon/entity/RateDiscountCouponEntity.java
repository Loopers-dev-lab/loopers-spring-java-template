package com.loopers.core.infra.database.mysql.coupon.entity;

import com.loopers.core.domain.order.RateDiscountCoupon;
import com.loopers.core.domain.order.vo.CouponDiscountRate;
import com.loopers.core.domain.order.vo.RateDiscountCouponId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Entity
@Table(
        name = "rate_discount_coupon",
        indexes = {
                @Index(name = "idx_rate_discount_coupon_coupon_id", columnList = "coupon_id"),
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RateDiscountCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long couponId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    public static RateDiscountCouponEntity of(CouponEntity couponEntity, RateDiscountCoupon rateDiscountCoupon) {
        return new RateDiscountCouponEntity(
                Optional.ofNullable(rateDiscountCoupon.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                couponEntity.getId(),
                rateDiscountCoupon.getRate().value()
        );
    }

    public RateDiscountCoupon to(CouponEntity couponEntity) {
        return RateDiscountCoupon.mappedBy(
                new RateDiscountCouponId(this.id.toString()),
                couponEntity.to(),
                new CouponDiscountRate(this.rate)
        );
    }
}
