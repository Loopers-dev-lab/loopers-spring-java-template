package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

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
        log.info("Coupon 조회 시도 - couponId: {}", couponId);
        Optional<Coupon> couponOpt = couponRepository.findById(couponId);
        if (couponOpt.isEmpty()) {
            log.error("Coupon을 찾을 수 없습니다 - couponId: {}", couponId);
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "[couponId = " + couponId + "] Coupon을 찾을 수 없습니다."
            );
        }
        Coupon coupon = couponOpt.get();
        log.info("Coupon 조회 성공 - couponId: {}, userId: {}, isUsed: {}, deletedAt: {}", 
                coupon.getId(), coupon.getUserId(), coupon.getIsUsed(), coupon.getDeletedAt());
        return coupon;
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
     * 쿠폰 ID를 받아서 검증하고 할인 금액을 계산하여 반환
     * @param orderId 주문 ID
     * @param userId 사용자 ID
     * @param totalPrice 주문 총액
     * @param couponId 쿠폰 ID
     * @return 할인 금액
     */
    @Transactional(noRollbackFor = CoreException.class)
    public BigDecimal useCoupon(Long orderId, Long userId, BigDecimal totalPrice, Long couponId) {
        log.info("쿠폰 사용 시도 - couponId: {}, orderId: {}, userId: {}", couponId, orderId, userId);

        // 1. 원자적 UPDATE로 쿠폰 사용 처리 (동시성 안전)
        long updatedRows = couponRepository.useCoupon(
                couponId,
                orderId,
                userId
        );
        log.info("쿠폰 사용 UPDATE 결과 - couponId: {}, updatedRows: {}", couponId, updatedRows);

        // 2. 실패했을 때만 상세 원인 파악 (Lazy Validation)
        if (updatedRows == 0) {
            log.warn("쿠폰 사용 실패 (updatedRows=0) - couponId: {}, 상세 원인 파악 시작", couponId);
            Coupon coupon = this.findById(couponId); // 이때 조회!
            
            // 소유자가 다른지?
            if (!coupon.getUserId().equals(userId)) {
                log.error("쿠폰 소유자가 다릅니다 - couponId: {}, 쿠폰 소유자: {}, 주문자: {}", 
                        couponId, coupon.getUserId(), userId);
                 throw new CoreException(ErrorType.BAD_REQUEST, "본인 쿠폰 아닙니다.");
            }
            // 이미 사용했는지?
            if (coupon.getIsUsed()) {
                log.error("이미 사용된 쿠폰입니다 - couponId: {}", couponId);
                 throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
            }
            
            log.error("사용 불가능한 쿠폰입니다 - couponId: {}", couponId);
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 불가능한 쿠폰입니다.");
        }

        // 3. 할인 계산
        log.info("쿠폰 할인 계산 시작 - couponId: {}, orderTotalPrice: {}", couponId, totalPrice);
        Coupon coupon = this.findById(couponId);
        BigDecimal couponDiscount = coupon.calculateDiscount(totalPrice);
        log.info("쿠폰 할인 계산 완료 - couponId: {}, 할인 금액: {}", couponId, couponDiscount);
        
        return couponDiscount;
    }

    /**
     * 주문에 사용된 쿠폰 원복 처리 (동시성 안전)
     * @param orderId 주문 ID
     */
    @Transactional
    public void rollbackCoupon(Long orderId) {
        couponRepository.rollbackCoupon(orderId);
    }
}

