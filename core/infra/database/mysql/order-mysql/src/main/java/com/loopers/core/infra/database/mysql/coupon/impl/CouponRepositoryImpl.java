package com.loopers.core.infra.database.mysql.coupon.impl;

import com.loopers.core.domain.error.DomainErrorCode;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.AmountDiscountCoupon;
import com.loopers.core.domain.order.Coupon;
import com.loopers.core.domain.order.RateDiscountCoupon;
import com.loopers.core.domain.order.repository.CouponRepository;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.infra.database.mysql.coupon.AmountDiscountCouponJpaRepository;
import com.loopers.core.infra.database.mysql.coupon.CouponJpaRepository;
import com.loopers.core.infra.database.mysql.coupon.RateDiscountCouponJpaRepository;
import com.loopers.core.infra.database.mysql.coupon.entity.AmountDiscountCouponEntity;
import com.loopers.core.infra.database.mysql.coupon.entity.CouponEntity;
import com.loopers.core.infra.database.mysql.coupon.entity.RateDiscountCouponEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {

    private final CouponJpaRepository couponRepository;
    private final RateDiscountCouponJpaRepository rateDiscountCouponRepository;
    private final AmountDiscountCouponJpaRepository amountDiscountCouponRepository;

    @Override
    public Coupon save(Coupon coupon) {
        if (coupon instanceof RateDiscountCoupon rateDiscountCoupon) {
            CouponEntity savedCouponEntity = couponRepository.save(CouponEntity.from(rateDiscountCoupon));

            return rateDiscountCouponRepository.save(RateDiscountCouponEntity.of(savedCouponEntity, rateDiscountCoupon))
                    .to(savedCouponEntity);
        }

        if (coupon instanceof AmountDiscountCoupon amountDiscountCoupon) {
            CouponEntity savedCouponEntity = couponRepository.save(CouponEntity.from(amountDiscountCoupon));

            return amountDiscountCouponRepository.save(AmountDiscountCouponEntity.of(savedCouponEntity, amountDiscountCoupon))
                    .to(savedCouponEntity);
        }

        throw new IllegalArgumentException(DomainErrorCode.NOT_SUPPORTED_COUPON_TYPE.getMessage());
    }

    @Override
    public Coupon getById(CouponId couponId) {
        CouponEntity coupon = couponRepository.findById(Long.parseLong(couponId.value()))
                .orElseThrow(() -> NotFoundException.withName("쿠폰"));

        Optional<RateDiscountCouponEntity> rateDiscountCoupon = rateDiscountCouponRepository.findByCouponId(coupon.getId());
        if (rateDiscountCoupon.isPresent()) {
            return rateDiscountCoupon.get().to(coupon);
        }

        Optional<AmountDiscountCouponEntity> amountDiscountCoupon = amountDiscountCouponRepository.findByCouponId(coupon.getId());
        if (amountDiscountCoupon.isPresent()) {
            return amountDiscountCoupon.get().to(coupon);
        }

        throw NotFoundException.withName("쿠폰");
    }

    @Override
    public Coupon getByIdWithLock(CouponId couponId) {
        CouponEntity coupon = couponRepository.findByIdWithLock(Long.parseLong(couponId.value()))
                .orElseThrow(() -> NotFoundException.withName("쿠폰"));

        Optional<RateDiscountCouponEntity> rateDiscountCoupon = rateDiscountCouponRepository.findByCouponId(coupon.getId());
        if (rateDiscountCoupon.isPresent()) {
            return rateDiscountCoupon.get().to(coupon);
        }

        Optional<AmountDiscountCouponEntity> amountDiscountCoupon = amountDiscountCouponRepository.findByCouponId(coupon.getId());
        if (amountDiscountCoupon.isPresent()) {
            return amountDiscountCoupon.get().to(coupon);
        }

        throw NotFoundException.withName("쿠폰");
    }
}
