package com.loopers.domain.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * 쿠폰 엔티티
 *
 * @author hyunjikoh
 * @since 2025. 11. 18.
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEntity extends BaseEntity {

    /**
     * 사용자 ID (users.id 참조)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 쿠폰 타입 (FIXED_AMOUNT: 정액 할인, PERCENTAGE: 배율 할인)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 20)
    private CouponType couponType;

    /**
     * 정액 할인 금액 (정액 쿠폰용)
     */
    @Column(name = "fixed_amount", precision = 10, scale = 2)
    private BigDecimal fixedAmount;

    /**
     * 할인 비율 (배율 쿠폰용, 0-100)
     */
    @Column(name = "percentage")
    private Integer percentage;

    /**
     * 쿠폰 상태 (UNUSED: 미사용, USED: 사용됨)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CouponStatus status = CouponStatus.UNUSED;

    @Version
    private Long version;


    /**
     * Create a fixed-amount coupon for the given user.
     *
     * @param userId      the identifier of the coupon owner
     * @param fixedAmount the fixed discount amount (must be greater than zero)
     * @throws IllegalArgumentException if {@code userId} is null, or if {@code fixedAmount} is null or not greater than zero
     */
    public CouponEntity(Long userId, BigDecimal fixedAmount) {
        validateUserId(userId);
        validateFixedAmount(fixedAmount);

        this.userId = userId;
        this.couponType = CouponType.FIXED_AMOUNT;
        this.fixedAmount = fixedAmount;
        this.percentage = null;
    }

    /**
     * Create a percentage-based coupon owned by the specified user.
     *
     * @param userId     the id of the coupon owner; must not be null
     * @param percentage the discount percentage (greater than 0 and no greater than 100)
     * @throws IllegalArgumentException if {@code userId} is null or {@code percentage} is null or not in the range (0, 100]
     */
    public CouponEntity(Long userId, Integer percentage) {
        validateUserId(userId);
        validatePercentage(percentage);

        this.userId = userId;
        this.couponType = CouponType.PERCENTAGE;
        this.fixedAmount = null;
        this.percentage = percentage;
    }

    /**
     * Create a fixed-amount coupon owned by the given user.
     *
     * <p>The coupon provides a fixed monetary discount applied at redemption.</p>
     *
     * @param user        the owner of the coupon; must not be null
     * @param fixedAmount the fixed discount amount; must be greater than zero
     * @return the created fixed-amount CouponEntity
     * @throws NullPointerException     if {@code user} or {@code fixedAmount} is null
     * @throws IllegalArgumentException if {@code fixedAmount} is not greater than zero or other validation fails
     */
    public static CouponEntity createFixedAmountCoupon(UserEntity user, BigDecimal fixedAmount) {
        Objects.requireNonNull(user, "유효하지 않은 사용자 입니다.");
        Objects.requireNonNull(fixedAmount, "정액 할인 금액은 필수입니다.");


        return new CouponEntity(user.getId(), fixedAmount);
    }

    /**
     * Create a percentage-based coupon for the given user.
     *
     * @param user       the owner of the coupon; must not be null
     * @param percentage the discount percentage; must be greater than 0 and less than or equal to 100
     * @return           the created percentage coupon entity
     * @throws IllegalArgumentException if the user or percentage is invalid
     */
    public static CouponEntity createPercentageCoupon(UserEntity user, Integer percentage) {
        Objects.requireNonNull(user, "유효하지 않은 사용자 ID입니다.");
        Objects.requireNonNull(percentage, "할인 비율은 필수입니다.");


        return new CouponEntity(user.getId(), percentage);
    }

    /**
     * Mark this coupon as used.
     *
     * @throws IllegalStateException if the coupon is already in `USED` status
     */
    public void use() {
        if (this.status == CouponStatus.USED) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        this.status = CouponStatus.USED;
    }

    /**
     * Checks whether the coupon is marked as used.
     *
     * @return `true` if the coupon status is `USED`, `false` otherwise
     */
    public boolean isUsed() {
        return this.status == CouponStatus.USED;
    }

    /**
     * Determines whether the coupon is available for use.
     *
     * @return `true` if the coupon is UNUSED, `false` otherwise.
     */
    public boolean canUse() {
        return this.status == CouponStatus.UNUSED;
    }

    /**
     * Calculate the discount amount applicable to a given product price based on this coupon.
     *
     * <p>For a fixed-amount coupon, the discount is the lesser of the coupon's fixed amount and the product price.
     * For a percentage coupon, the discount is productPrice * percentage / 100, rounded half-up to the nearest whole unit.</p>
     *
     * @param productPrice the product price; must be greater than zero
     * @return the calculated discount amount
     * @throws IllegalArgumentException if {@code productPrice} is null or less than or equal to zero
     */
    public BigDecimal calculateDiscount(BigDecimal productPrice) {
        if (productPrice == null || productPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }

        return switch (this.couponType) {
            case FIXED_AMOUNT -> this.fixedAmount.min(productPrice);
            case PERCENTAGE -> productPrice.multiply(BigDecimal.valueOf(this.percentage))
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
        };
    }

    /**
     * Validates that the provided user ID is the owner of this coupon.
     *
     * @param userId the user ID to validate against the coupon's owner
     * @throws IllegalArgumentException if the provided `userId` does not match the coupon owner
     */
    public void validateOwnership(Long userId) {
        if (!Objects.equals(this.userId, userId)) {
            throw new IllegalArgumentException("쿠폰 소유자만 사용할 수 있습니다.");
        }
    }

    /**
     * Validates that the user identifier is not null.
     *
     * @param userId the user identifier to validate
     * @throws IllegalArgumentException if {@code userId} is null
     */
    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * Validates that the fixed-amount discount is greater than zero.
     *
     * @param fixedAmount the discount amount to validate
     * @throws IllegalArgumentException if `fixedAmount` is null or less than or equal to zero
     */
    private static void validateFixedAmount(BigDecimal fixedAmount) {
        if (fixedAmount == null || fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("정액 할인 금액은 0보다 커야 합니다.");
        }
    }

    /**
     * Validates that a percentage value is greater than 0 and less than or equal to 100.
     *
     * @param percentage the percentage to validate
     * @throws IllegalArgumentException if `percentage` is null, less than or equal to 0, or greater than 100
     */
    private static void validatePercentage(Integer percentage) {
        if (percentage == null || percentage <= 0 || percentage > 100) {
            throw new IllegalArgumentException("할인 비율은 0보다 크고 100 이하여야 합니다.");
        }
    }

    /**
     * Enforces coupon-type-specific invariants on the entity's fields.
     *
     * <p>Validates that:
     * - For FIXED_AMOUNT coupons, `fixedAmount` is present and greater than zero and `percentage` is null.
     * - For PERCENTAGE coupons, `percentage` is present and between 1 and 100 (inclusive) and `fixedAmount` is null.
     *
     * @throws IllegalStateException if any of the above invariants are violated
     */
    @Override
    protected void guard() {
        // 쿠폰 타입별 필수 필드 검증
        switch (this.couponType) {
            case FIXED_AMOUNT -> {
                if (this.fixedAmount == null || this.fixedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException("정액 쿠폰은 할인 금액이 필수입니다.");
                }
                if (this.percentage != null) {
                    throw new IllegalStateException("정액 쿠폰은 할인 비율을 가질 수 없습니다.");
                }
            }
            case PERCENTAGE -> {
                if (this.percentage == null || this.percentage <= 0 || this.percentage > 100) {
                    throw new IllegalStateException("배율 쿠폰은 0-100 범위의 할인 비율이 필수입니다.");
                }
                if (this.fixedAmount != null) {
                    throw new IllegalStateException("배율 쿠폰은 정액 할인 금액을 가질 수 없습니다.");
                }
            }
        }
    }

    /**
     * Reverts the coupon to the unused state.
     *
     * Sets the coupon's status to `UNUSED` so the coupon can be applied again.
     */
    public void revert() {
        this.status = CouponStatus.UNUSED;
    }
}