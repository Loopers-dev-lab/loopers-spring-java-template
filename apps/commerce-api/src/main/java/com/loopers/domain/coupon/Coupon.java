package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Coupon extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private CouponType couponType;

    private BigDecimal discountValue;

    private Boolean isUsed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Builder
    private Coupon(
            CouponType couponType,
            BigDecimal discountValue,
            User user
    ) {
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.user = user;
        guard();
    }

    /**
     * 쿠폰 할인 금액 계산
     * @param totalPrice 할인을 적용할 총 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscount(BigDecimal totalPrice) {
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "Coupon : 할인을 적용할 금액이 올바르지 않습니다."
            );
        }

        // if문 없이 enum의 메소드 호출
        return couponType.calculateDiscount(this.discountValue, totalPrice);
    }

    @Override
    protected void guard() {
        // couponType 검증
        if (couponType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : couponType이 비어있을 수 없습니다.");
        }

        // discountValue 검증
        if (discountValue == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : discountValue가 비어있을 수 없습니다.");
        }else if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : discountValue는 음수가 될 수 없습니다.");
        }

        // PERCENTAGE 타입인 경우 할인율 검증
        if (couponType == CouponType.PERCENTAGE && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : 할인율은 100을 초과할 수 없습니다.");
        }

        // isUsed 검증
        if (isUsed == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : isUsed가 비어있을 수 없습니다.");
        }

        // isUsed와 order의 관계 검증
        // isUsed가 false면 order는 null이어야 함
        if (!isUsed && order != null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : 미사용 쿠폰은 order가 null이어야 합니다.");
        }
        // isUsed가 true면 order는 null이면 안 됨
        if (isUsed && order == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : 사용된 쿠폰은 order가 비어있을 수 없습니다.");
        }

        // user 검증: null이 아니어야 함
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Coupon : user가 비어있을 수 없습니다.");
        }
    }
}

