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
     * UserCoupon 인스턴스를 생성합니다.
     *
     * @param userId 사용자 ID (필수)
     * @param coupon 쿠폰 (필수)
     * @throws CoreException 유효성 검증 실패 시
     */
    public UserCoupon(Long userId, Coupon coupon) {
        validateUserId(userId);
        validateCoupon(coupon);
        this.userId = userId;
        this.coupon = coupon;
        this.isUsed = false;
    }

    /**
     * UserCoupon 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param userId 사용자 ID
     * @param coupon 쿠폰
     * @return 생성된 UserCoupon 인스턴스
     * @throws CoreException 유효성 검증 실패 시
     */
    public static UserCoupon of(Long userId, Coupon coupon) {
        return new UserCoupon(userId, coupon);
    }

    /**
     * 사용자 ID의 유효성을 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws CoreException userId가 null일 경우
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    /**
     * 쿠폰의 유효성을 검증합니다.
     *
     * @param coupon 검증할 쿠폰
     * @throws CoreException coupon이 null일 경우
     */
    private void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다.");
        }
    }

    /**
     * 쿠폰을 사용합니다.
     * <p>
     * 이미 사용된 쿠폰은 다시 사용할 수 없습니다.
     * </p>
     *
     * @throws CoreException 이미 사용된 쿠폰일 경우
     */
    public void use() {
        if (this.isUsed) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        this.isUsed = true;
    }

    /**
     * 쿠폰이 사용 가능한지 확인합니다.
     *
     * @return 사용 가능하면 true, 아니면 false
     */
    public boolean isAvailable() {
        return !this.isUsed;
    }

    /**
     * 쿠폰 코드를 반환합니다.
     *
     * @return 쿠폰 코드
     */
    public String getCouponCode() {
        return coupon.getCode();
    }

    /**
     * 쿠폰을 반환합니다.
     *
     * @return 쿠폰
     */
    public Coupon getCoupon() {
        return coupon;
    }
}

