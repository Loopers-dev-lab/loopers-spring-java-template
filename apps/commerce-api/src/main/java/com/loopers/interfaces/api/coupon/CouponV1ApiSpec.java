package com.loopers.interfaces.api.coupon;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Coupon V1 API", description = "쿠폰 관리 API")
public interface CouponV1ApiSpec {

    @Operation(
            summary = "쿠폰 발급",
            description = "사용자에게 쿠폰을 발급합니다."
    )
    ApiResponse<CouponV1Dto.CouponResponse> issueCoupon(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.HEADER)
            String userId,
            @RequestBody(description = "쿠폰 발급 요청", required = true)
            CouponV1Dto.IssueRequest request
    );

    @Operation(
            summary = "쿠폰 사용",
            description = "쿠폰을 사용 처리합니다."
    )
    ApiResponse<CouponV1Dto.CouponResponse> useCoupon(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.HEADER)
            String userId,
            @Parameter(description = "쿠폰 ID", required = true, in = ParameterIn.PATH)
            Long couponId
    );

    @Operation(
            summary = "쿠폰 조회",
            description = "쿠폰 정보를 조회합니다."
    )
    ApiResponse<CouponV1Dto.CouponResponse> getCoupon(
            @Parameter(description = "쿠폰 ID", required = true, in = ParameterIn.PATH)
            Long couponId
    );

    @Operation(
            summary = "내 쿠폰 목록 조회",
            description = "사용자의 모든 쿠폰 목록을 조회합니다."
    )
    ApiResponse<CouponV1Dto.CouponListResponse> getMyCoupons(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.HEADER)
            String userId
    );

    @Operation(
            summary = "사용 가능한 쿠폰 목록 조회",
            description = "사용자의 사용 가능한 쿠폰 목록을 조회합니다."
    )
    ApiResponse<CouponV1Dto.CouponListResponse> getAvailableCoupons(
            @Parameter(description = "사용자 ID", required = true, in = ParameterIn.HEADER)
            String userId
    );

    @Operation(
            summary = "할인 금액 계산",
            description = "쿠폰의 할인 금액을 계산합니다."
    )
    ApiResponse<CouponV1Dto.CalculateDiscountResponse> calculateDiscount(
            @Parameter(description = "쿠폰 ID", required = true, in = ParameterIn.PATH)
            Long couponId,
            @RequestBody(description = "할인 금액 계산 요청", required = true)
            CouponV1Dto.CalculateDiscountRequest request
    );
}
