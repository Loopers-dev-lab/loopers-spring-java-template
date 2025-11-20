package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 쿠폰 도메인 엔티티.
 * <p>
 * 사용자가 소유한 쿠폰과 사용 여부를 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "user_coupon", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_coupon_user_coupon", columnNames = {"ref_user_id", "ref_coupon_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserCoupon extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Create a new UserCoupon linking a user to a coupon and marking it as unused.
     *
     * @param userId the ID of the user owning the coupon; must not be null
     * @param coupon the Coupon to associate; must not be null
     * @throws CoreException if either {@code userId} or {@code coupon} is null (validation failure)
     */
    public UserCoupon(Long userId, Coupon coupon) {
        validateUserId(userId);
        validateCoupon(coupon);
        this.userId = userId;
        this.coupon = coupon;
        this.isUsed = false;
    }

    /**
     * Create a UserCoupon for the given user and coupon.
     *
     * @param userId the owner's user id
     * @param coupon the coupon to associate with the user
     * @return the created UserCoupon instance
     * @throws CoreException if `userId` or `coupon` is null
     */
    public static UserCoupon of(Long userId, Coupon coupon) {
        return new UserCoupon(userId, coupon);
    }

    /**
     * Ensures the provided user ID is not null.
     *
     * @throws CoreException if `userId` is null (error type BAD_REQUEST).
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    /**
     * Validates that the provided Coupon is not null.
     *
     * @param coupon the Coupon to validate; must not be null
     * @throws CoreException if {@code coupon} is null with ErrorType.BAD_REQUEST
     */
    private void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다.");
        }
    }

    /**
     * Mark the coupon as used.
     *
     * @throws CoreException if the coupon has already been used
     */
    public void use() {
        if (this.isUsed) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.isUsed = true;
    }

    /**
     * Determines whether the coupon is available for use.
     *
     * @return true if the coupon has not been used, false otherwise.
     */
    public boolean isAvailable() {
        return !this.isUsed;
    }

    /**
     * Retrieves the associated coupon's code.
     *
     * @return the associated coupon's code
     */
    public String getCouponCode() {
        return coupon.getCode();
    }

    /**
     * Accesses the Coupon associated with this UserCoupon.
     *
     * @return the associated Coupon entity
     */
    public Coupon getCoupon() {
        return coupon;
    }
}
