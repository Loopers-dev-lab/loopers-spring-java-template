package com.loopers.domain.issuedcoupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "issued_coupons")
@NoArgsConstructor
@Getter
public class IssuedCoupon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CouponStatus status;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Builder
    protected IssuedCoupon(User user, Coupon coupon, CouponStatus status) {
        validateIssuedCouponFields(user, coupon, status);

        this.user = user;
        this.coupon = coupon;
        this.status = status;
    }

    public static IssuedCoupon issue(User user, Coupon coupon) {
        validateCanIssue(coupon);

        return IssuedCoupon.builder()
                .user(user)
                .coupon(coupon)
                .status(CouponStatus.USABLE)
                .build();
    }

    public void useCoupon() {
        validateCanUseCoupon();

        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * 쿠폰 복구 (보상 트랜잭션)
     * 결제 실패 시 사용된 쿠폰을 다시 사용 가능한 상태로 복구
     */
    public void restoreCoupon() {
        if (this.status != CouponStatus.USED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용된 쿠폰만 복구할 수 있습니다");
        }

        this.status = CouponStatus.USABLE;
        this.usedAt = null;
    }

    private static void validateIssuedCouponFields(User user, Coupon coupon, CouponStatus status) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 필수입니다");
        }

        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다");
        }

        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 상태는 필수입니다");
        }
    }

    private static void validateCanIssue(Coupon coupon) {
        if (coupon == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰은 필수입니다");
        }

        if (!coupon.isActive()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비활성화된 쿠폰입니다");
        }
    }

    private void validateCanUseCoupon() {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 상태가 올바르지 않습니다");
        }

        if (status == CouponStatus.USED) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용된 쿠폰입니다");
        }

        if (status == CouponStatus.EXPIRED) {
            throw new CoreException(ErrorType.CONFLICT, "이미 만료된 쿠폰입니다");
        }

        if (usedAt != null) {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용된 쿠폰입니다");
        }
    }

}
