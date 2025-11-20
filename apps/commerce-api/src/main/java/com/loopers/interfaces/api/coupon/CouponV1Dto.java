package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.UserCoupon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class CouponV1Dto {

    public record CreateCouponRequest(
        @NotBlank(message = "쿠폰명은 필수입니다.")
        String name,
        @NotNull(message = "쿠폰 타입은 필수입니다.")
        CouponType type,
        @NotNull(message = "할인 값은 필수입니다.")
        @Positive(message = "할인 값은 0보다 커야 합니다.")
        BigDecimal discountValue,
        String description
    ) {
    }

    public record CouponResponse(
        Long id,
        String name,
        CouponType type,
        BigDecimal discountValue,
        String description
    ) {
        public static CouponResponse from(Coupon coupon) {
            return new CouponResponse(
                coupon.getId(),
                coupon.getName(),
                coupon.getType(),
                coupon.getDiscountValue(),
                coupon.getDescription()
            );
        }
    }

    public record UserCouponResponse(
        Long id,
        String userId,
        CouponResponse coupon,
        boolean isUsed,
        ZonedDateTime usedAt,
        ZonedDateTime createdAt
    ) {
        public static UserCouponResponse from(UserCoupon userCoupon) {
            return new UserCouponResponse(
                userCoupon.getId(),
                userCoupon.getUserId(),
                CouponResponse.from(userCoupon.getCoupon()),
                userCoupon.isUsed(),
                userCoupon.getUsedAt(),
                userCoupon.getCreatedAt()
            );
        }
    }
}
