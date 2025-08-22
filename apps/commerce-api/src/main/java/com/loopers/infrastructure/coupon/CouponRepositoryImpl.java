package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.embeded.CouponUserId;
import com.loopers.domain.coupon.embeded.CouponOrderId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CouponRepositoryImpl implements CouponRepository {
    
    private final CouponJpaRepository couponJpaRepository;
    
    public CouponRepositoryImpl(CouponJpaRepository couponJpaRepository) {
        this.couponJpaRepository = couponJpaRepository;
    }
    
    @Override
    public CouponModel save(CouponModel coupon) {
        return couponJpaRepository.save(coupon);
    }
    
    @Override
    public Optional<CouponModel> findById(Long id) {
        return couponJpaRepository.findById(id);
    }
    
    @Override
    public List<CouponModel> findByUserId(Long userId) {
        return couponJpaRepository.findByUserId(CouponUserId.of(userId));
    }
    
    @Override
    public List<CouponModel> findUsableCouponsByUserId(Long userId) {
        return couponJpaRepository.findUsableCouponsByUserId(CouponUserId.of(userId));
    }
    
    @Override
    public List<CouponModel> findByOrderId(Long orderId) {
        return couponJpaRepository.findByOrderId(CouponOrderId.of(orderId));
    }
    
    @Override
    public boolean existsById(Long id) {
        return couponJpaRepository.existsById(id);
    }
    
    @Override
    public Optional<CouponModel> findByIdForUpdate(Long id) {
        return couponJpaRepository.findByIdForUpdate(id);
    }
    
    @Override
    public void deleteAll() {
        couponJpaRepository.deleteAll();
    }

    @Override
    public Optional<CouponModel> findByIdAndUserId(Long id, Long userId) {
        return couponJpaRepository.findByIdAndUserId_UserId(id, userId);
    }
}
