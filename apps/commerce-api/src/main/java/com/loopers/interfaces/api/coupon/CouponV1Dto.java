package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CouponV1Dto {

    /**
     * 쿠폰 발급 요청
     */
    public record IssueRequest(
            @NotBlank(message = "쿠폰 이름은 필수입니다.")
            String couponName,

            @NotNull(message = "쿠폰 타입은 필수입니다.")
            CouponType couponType,

            @NotNull(message = "할인 값은 필수입니다.")
            @Positive(message = "할인 값은 0보다 커야 합니다.")
            BigDecimal discountValue,

            @NotNull(message = "만료 시각은 필수입니다.")
            LocalDateTime expiresAt
    ) {
    }

    /**
     * 쿠폰 응답
     */
    public record CouponResponse(
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
        public static CouponResponse from(Coupon coupon) {
            return new CouponResponse(
                    coupon.getId(),
                    coupon.getUserId(),
                    coupon.getCouponName(),
                    coupon.getCouponType(),
                    coupon.getDiscountValue(),
                    coupon.isUsed(),
                    coupon.getUsedAt(),
                    coupon.getIssuedAt(),
                    coupon.getExpiresAt()
            );
        }
    }

    /**
     * 쿠폰 목록 응답
     */
    public record CouponListResponse(
            List<CouponResponse> coupons
    ) {
        public static CouponListResponse from(List<Coupon> coupons) {
            List<CouponResponse> responses = coupons.stream()
                    .map(CouponResponse::from)
                    .collect(Collectors.toList());
            return new CouponListResponse(responses);
        }
    }

    /**
     * 할인 금액 계산 요청
     */
    public record CalculateDiscountRequest(
            @NotNull(message = "원가는 필수입니다.")
            @Positive(message = "원가는 0보다 커야 합니다.")
            BigDecimal originalAmount
    ) {
    }

    /**
     * 할인 금액 계산 응답
     */
    public record CalculateDiscountResponse(
            BigDecimal discountAmount
    ) {
        public static CalculateDiscountResponse from(BigDecimal discountAmount) {
            return new CalculateDiscountResponse(discountAmount);
        }
    }
}
