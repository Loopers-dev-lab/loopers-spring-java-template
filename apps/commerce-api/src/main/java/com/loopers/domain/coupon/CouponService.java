package com.loopers.domain.coupon;

import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponService {

    private final CouponRepository couponRepository;

    /**
     * 쿠폰 저장
     */
    @Transactional
    public Optional<Coupon> saveCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 조회
     */
    @Transactional(readOnly = true)
    public Coupon findById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[couponId = " + couponId + "] Coupon을 찾을 수 없습니다."
                ));
    }

    /**
     * 사용자의 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Coupon> findByUserId(Long userId) {
        return couponRepository.findByUserId(userId);
    }

    /**
     * 사용자의 미사용 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Coupon> findAvailableCouponsByUserId(Long userId) {
        return couponRepository.findByUserIdAndIsUsedFalse(userId);
    }

    /**
     * 주문에 쿠폰 적용 (동시성 안전)
     * 쿠폰 ID를 받아서 검증하고 할인을 적용
     * @param order 주문
     * @param couponId 쿠폰 ID
     */
    @Transactional
    public void useCoupon(Order order, Long couponId) {

        // 1. 원자적 UPDATE로 쿠폰 사용 처리 (동시성 안전)
        long updatedRows = couponRepository.useCoupon(
                couponId,
                order.getId(),
                order.getUserId()
        );

        // 2. 실패했을 때만 상세 원인 파악 (Lazy Validation)
        if (updatedRows == 0) {
            Coupon coupon = this.findById(couponId); // 이때 조회!
            
            // 소유자가 다른지?
            if (!coupon.getUserId().equals(order.getUserId())) {
                 throw new CoreException(ErrorType.BAD_REQUEST, "본인 쿠폰 아닙니다.");
            }
            // 이미 사용했는지?
            if (coupon.getIsUsed()) {
                 throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
            }
            
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 불가능한 쿠폰입니다.");
        }

        // 3. 할인 계산 및 적용
        Coupon coupon = this.findById(couponId);
        BigDecimal couponDiscount = coupon.calculateDiscount(order.getTotalPrice());
        order.applyDiscount(couponDiscount);
    }
}

