package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CouponCommand {
    
    public record CreateFixedCouponRequest(
            Long userId,
            BigDecimal amount,
            LocalDateTime expiredAt) {
    }
    
    public record CreateRateCouponRequest(
            Long userId,
            BigDecimal rate,
            LocalDateTime expiredAt) {
    }
    
    public record UseCouponRequest(
            Long couponId,
            Long orderId,
            BigDecimal orderAmount) {
    }
    
    public record CouponResponse(
            Long couponId,
            Long userId,
            String type,
            BigDecimal value,
            boolean used,
            Long orderId,
            LocalDateTime issuedAt,
            LocalDateTime expiredAt,
            boolean canUse) {
        
        public static CouponResponse from(CouponModel coupon) {
            return new CouponResponse(
                    coupon.getId(),
                    coupon.getUserId().getValue(),
                    coupon.getType().getValue().name(),
                    coupon.getValue().getValue(),
                    coupon.getUsed().isUsed(),
                    coupon.getOrderId() != null ? coupon.getOrderId().getValue() : null,
                    coupon.getIssuedAt().getValue(),
                    coupon.getExpiredAt().getValue(),
                    coupon.canUse()
            );
        }
        
        public static List<CouponResponse> fromList(List<CouponModel> coupons) {
            return coupons.stream()
                    .map(CouponResponse::from)
                    .toList();
        }
    }
    
    public record DiscountCalculationResponse(
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            CouponResponse appliedCoupon) {
        
        public static DiscountCalculationResponse of(BigDecimal originalAmount, CouponModel coupon) {
            BigDecimal discountAmount = coupon.calculateDiscountAmount(originalAmount);
            BigDecimal finalAmount = originalAmount.subtract(discountAmount);
            return new DiscountCalculationResponse(
                    originalAmount,
                    discountAmount,
                    finalAmount,
                    CouponResponse.from(coupon)
            );
        }
    }
    public record Result(
            CouponModel coupon,
            BigDecimal discountAmount
    ) {
        public boolean isFixed() {
            return coupon.getType().isFixed();
        }

        public boolean isRate() {
            return coupon.getType().isRate();
        }
    }
}
