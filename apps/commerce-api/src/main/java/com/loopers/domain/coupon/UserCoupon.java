package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false)
    private boolean isUsed;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder
    private UserCoupon(String userId, Coupon coupon) {
        validateUserId(userId);
        validateCoupon(coupon);

        this.userId = userId;
        this.coupon = coupon;
        this.isUsed = false;
        this.usedAt = null;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    private void validateCoupon(Coupon coupon) {
        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다.");
        }
    }

    /**
     * 특정 사용자가 이 쿠폰을 사용할 수 있는지 검증하고 사용 처리합니다.
     *
     * @param userId 사용자 ID
     * @throws CoreException 소유자가 아니거나 사용 불가능한 경우
     */
    public void useBy(String userId) {
        validateOwnership(userId);
        validateAvailability();
        use();
    }

    /**
     * 쿠폰 소유자인지 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @throws CoreException 소유자가 아닌 경우
     */
    public void validateOwnership(String userId) {
        if (!this.userId.equals(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰만 사용할 수 있습니다.");
        }
    }

    /**
     * 쿠폰 사용 가능 여부를 검증합니다.
     *
     * @throws CoreException 사용 불가능한 경우
     */
    public void validateAvailability() {
        if (!isAvailable()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }
    }

    /**
     * 쿠폰을 사용합니다.
     *
     * @deprecated 대신 {@link #useBy(String)}를 사용하세요.
     */
    @Deprecated
    public void use() {
        if (isUsed) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
        }
        if (this.getDeletedAt() != null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "삭제된 쿠폰입니다.");
        }

        this.isUsed = true;
        this.usedAt = ZonedDateTime.now();
    }

    /**
     * 쿠폰이 사용 가능한지 확인합니다.
     *
     * @return 사용 가능 여부
     */
    public boolean isAvailable() {
        return !isUsed && this.getDeletedAt() == null;
    }

    /**
     * 주문 금액에 대한 할인 금액을 계산합니다.
     *
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        return coupon.calculateDiscountAmount(orderAmount);
    }
}
