package com.loopers.domain.coupon;

import com.loopers.domain.order.Order;
import com.loopers.domain.user.User;
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
    public List<Coupon> findByUser(User user) {
        return couponRepository.findByUser(user);
    }

    /**
     * 사용자의 미사용 쿠폰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Coupon> findAvailableCouponsByUser(User user) {
        return couponRepository.findByUserAndIsUsedFalse(user);
    }

    /**
     * 주문에 쿠폰 적용 (동시성 안전)
     * 쿠폰 ID를 받아서 검증하고 할인을 적용
     * @param order 주문
     * @param couponId 쿠폰 ID
     */
    @Transactional
    public void useCoupon(Order order, Long couponId) {
        // 1. 쿠폰 정보 조회 (할인 계산용)
        Coupon coupon = this.findById(couponId);

        // 2. 쿠폰 소유자 확인
        if (!coupon.getUser().getId().equals(order.getUser().getId())) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "[couponId = " + couponId + "] 본인의 쿠폰만 사용할 수 있습니다."
            );
        }

        // 3. 원자적 UPDATE로 쿠폰 사용 처리 (동시성 안전)
        long updatedRows = couponRepository.useCoupon(
                couponId,
                order.getId(),
                order.getUser().getId()
        );

        if (updatedRows == 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "[couponId = " + couponId + "] 쿠폰을 사용할 수 없습니다. (이미 사용됨 or 삭제됨 or 주문 상태 오류)"
            );
        }

        // 4. 할인 계산 및 적용
        BigDecimal couponDiscount = coupon.calculateDiscount(order.getTotalPrice());
        order.applyDiscount(couponDiscount);
    }
}

