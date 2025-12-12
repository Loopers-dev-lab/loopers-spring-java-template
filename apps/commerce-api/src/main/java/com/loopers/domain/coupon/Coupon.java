package com.loopers.domain.coupon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 쿠폰 도메인 모델
 * - 순수 도메인 객체 (JPA 의존성 없음)
 * - 사용자에게 발급된 쿠폰 (UserCoupon)
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Coupon {

    private Long id;
    private String userId;           // 쿠폰 소유자
    private String couponName;       // 쿠폰 이름
    private CouponType couponType;   // 쿠폰 타입 (정액/정률)
    private BigDecimal discountValue; // 할인 값 (정액: 금액, 정률: 퍼센트)
    private boolean isUsed;          // 사용 여부
    private LocalDateTime usedAt;    // 사용 시각
    private LocalDateTime issuedAt;  // 발급 시각
    private LocalDateTime expiresAt; // 만료 시각

    /**
     * 쿠폰 발급 팩토리 메서드
     */
    public static Coupon issue(
            String userId,
            String couponName,
            CouponType couponType,
            BigDecimal discountValue,
            LocalDateTime expiresAt
    ) {
        validateUserId(userId);
        validateCouponName(couponName);
        validateCouponType(couponType);
        validateDiscountValue(couponType, discountValue);
        validateExpiresAt(expiresAt);

        LocalDateTime now = LocalDateTime.now();

        return new Coupon(
                null,
                userId,
                couponName,
                couponType,
                discountValue,
                false,
                null,
                now,
                expiresAt
        );
    }

    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Coupon reconstruct(
            Long id,
            String userId,
            String couponName,
            CouponType couponType,
            BigDecimal discountValue,
            boolean isUsed,
            LocalDateTime usedAt,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        return new Coupon(
                id,
                userId,
                couponName,
                couponType,
                discountValue,
                isUsed,
                usedAt,
                issuedAt,
                expiresAt
        );
    }

    /**
     * 쿠폰 사용
     * - 쿠폰을 사용 처리하고 사용 시각을 기록
     */
    public void use() {
        if (!canUse()) {
            throw new IllegalStateException("사용할 수 없는 쿠폰입니다");
        }

        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     * - 사용되지 않았고, 만료되지 않았어야 함
     */
    public boolean canUse() {
        return !isUsed && !isExpired();
    }

    /**
     * 쿠폰 만료 여부 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 쿠폰 소유자 확인
     */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    /**
     * 할인 금액 계산
     * - 정액: 할인 값 그대로 반환
     * - 정률: 원가의 할인율만큼 계산 (소수점 버림)
     */
    public BigDecimal calculateDiscount(BigDecimal originalAmount) {
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("원가는 0 이상이어야 합니다");
        }

        BigDecimal discount;
        if (couponType == CouponType.FIXED) {
            // 정액: 할인 값 그대로
            discount = discountValue;
        } else {
            // 정률: 원가 * (할인율 / 100)
            discount = originalAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        }

        // 할인 금액은 원가를 초과할 수 없음
        if (discount.compareTo(originalAmount) > 0) {
            return originalAmount;
        }

        return discount;
    }

    // === Validation Methods ===

    private static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }

    private static void validateCouponName(String couponName) {
        if (couponName == null || couponName.trim().isEmpty()) {
            throw new IllegalArgumentException("쿠폰 이름은 필수입니다");
        }
    }

    private static void validateCouponType(CouponType couponType) {
        if (couponType == null) {
            throw new IllegalArgumentException("쿠폰 타입은 필수입니다");
        }
    }

    private static void validateDiscountValue(CouponType couponType, BigDecimal discountValue) {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 값은 0보다 커야 합니다");
        }

        // 정률 쿠폰은 100% 이하여야 함
        if (couponType == CouponType.RATE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("정률 쿠폰의 할인율은 100% 이하여야 합니다");
        }
    }

    private static void validateExpiresAt(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("만료 시각은 필수입니다");
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("만료 시각은 현재 시각 이후여야 합니다");
        }
    }
}
