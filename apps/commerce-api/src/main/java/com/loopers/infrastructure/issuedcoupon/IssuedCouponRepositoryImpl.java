package com.loopers.infrastructure.issuedcoupon;

import com.loopers.domain.issuedcoupon.IssuedCoupon;
import com.loopers.domain.issuedcoupon.IssuedCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class IssuedCouponRepositoryImpl implements IssuedCouponRepository {

    private final IssuedCouponJpaRepository issuedCouponJpaRepository;

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        return issuedCouponJpaRepository.save(issuedCoupon);
    }
}
