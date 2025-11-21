package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Getter
@Table(name = "coupons")
public class Coupon extends BaseEntity {
    @Column(name = "coupon_code", nullable = false, unique = true)
    private String code;

    @Column(name = "coupon_name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "valid_start_date", nullable = false)
    private String validStartDate;

    @Column(name = "valid_end_date", nullable = false)
    private String validEndDate;

    @Embedded
    private DiscountPolicy discountPolicy;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;

    private Integer maxIssuanceLimit;  // 총 발행 가능한 수량 (null이면 무제한)

    @Column(name = "current_issuance_count", nullable = false, columnDefinition = "int default 0")
    private Integer currentIssuanceCount = 0; // 현재까지 발행된 수량

    @Builder
    protected Coupon(String code, String name, String description, String validStartDate, String validEndDate, DiscountPolicy discountPolicy) {
        validateCouponCode(code);
        validateCouponName(name);
        validateDiscountPolicy(discountPolicy);
        validateCouponDates(validStartDate, validEndDate);

        this.code = code;
        this.name = name;
        this.description = description;
        this.validStartDate = validStartDate;
        this.validEndDate = validEndDate;
        this.discountPolicy = discountPolicy;
        this.currentIssuanceCount = 0;
    }

    public void increaseIssuanceCount() {
        if (maxIssuanceLimit != null && currentIssuanceCount >= maxIssuanceLimit) {
            throw new CoreException(ErrorType.CONFLICT, "쿠폰이 모두 소진되었습니다.");
        }
        this.currentIssuanceCount++;
    }

    private static void validateCouponCode(String code) {
        if (code == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰코드는 필수값 입니다");
        }

        if (!code.matches("^[A-Z0-9]{10,20}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰코드는 대문자 영문과 숫자로 10~20자여야 합니다");
        }
    }

    private static void validateCouponName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수값 입니다");
        }
    }

    private static void validateDiscountPolicy(DiscountPolicy discountPolicy) {
        if (discountPolicy == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 정보는 필수값 입니다");
        }

        if (discountPolicy.getDiscountType() == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 방식은 필수값 입니다");
        }

        if (discountPolicy.getDiscountValue() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인률(액)은 0보다 큰 양수여야 합니다");
        }
    }

    private static void validateCouponDates(String validStartDate, String validEndDate) {
        if (validStartDate == null || validEndDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효일은 필수값 입니다");
        }
    }

}
