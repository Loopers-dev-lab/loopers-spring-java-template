package com.loopers.interfaces.api.coupon;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Coupon API Spec", description = "Coupon V1 API Spec")
public interface CouponV1ApiSpec {
    @Operation(summary = "쿠폰 발급")
    ApiResponse<CouponV1Dto.CouponResponse> issueCoupon(CouponV1Dto.CouponRequest request);

    @Operation(summary = "사용 가능 쿠폰 조회")
    ApiResponse<List<CouponV1Dto.CouponResponse>> findCoupons(Long userId);
}
