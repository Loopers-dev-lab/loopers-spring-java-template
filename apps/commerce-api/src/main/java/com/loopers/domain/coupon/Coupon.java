package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.interfaces.api.coupon.CouponV1Dto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponType couponType;

    @Column(nullable = false)
    private int quantity;

    public Coupon(Long userId, CouponType couponType, int quantity) {
        this.userId = userId;
        this.couponType = couponType;
        this.quantity = quantity;
    }

    public static Coupon create(CouponV1Dto.CouponRequest request) {
        return new Coupon(
                request.userId(),
                request.couponType(),
                request.quantity()
        );
    }

    public void useCoupon() {
        this.quantity--;
    }

}
