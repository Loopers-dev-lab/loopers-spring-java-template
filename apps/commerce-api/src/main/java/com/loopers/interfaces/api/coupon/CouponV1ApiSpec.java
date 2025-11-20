package com.loopers.interfaces.api.coupon;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Coupon V1 API", description = "쿠폰 관리 API")
public interface CouponV1ApiSpec {

    @Operation(
        summary = "쿠폰 생성",
        description = "새로운 쿠폰을 생성합니다."
    )
    ApiResponse<CouponV1Dto.CouponResponse> createCoupon(
        CouponV1Dto.CreateCouponRequest request
    );

    @Operation(
        summary = "쿠폰 조회",
        description = "쿠폰 ID로 쿠폰 정보를 조회합니다."
    )
    ApiResponse<CouponV1Dto.CouponResponse> getCoupon(
        @Schema(description = "쿠폰 ID")
        Long couponId
    );

    @Operation(
        summary = "전체 쿠폰 목록 조회",
        description = "모든 쿠폰 목록을 조회합니다."
    )
    ApiResponse<List<CouponV1Dto.CouponResponse>> getAllCoupons();

    @Operation(
        summary = "사용자에게 쿠폰 발급",
        description = "특정 사용자에게 쿠폰을 발급합니다."
    )
    ApiResponse<CouponV1Dto.UserCouponResponse> issueCouponToUser(
        @Schema(description = "사용자 ID")
        String userId,
        @Schema(description = "쿠폰 ID")
        Long couponId
    );

    @Operation(
        summary = "사용자 쿠폰 목록 조회",
        description = "특정 사용자가 보유한 모든 쿠폰을 조회합니다."
    )
    ApiResponse<List<CouponV1Dto.UserCouponResponse>> getUserCoupons(
        @Schema(description = "사용자 ID")
        String userId
    );

    @Operation(
        summary = "사용 가능한 쿠폰 목록 조회",
        description = "특정 사용자가 사용 가능한 쿠폰 목록을 조회합니다."
    )
    ApiResponse<List<CouponV1Dto.UserCouponResponse>> getAvailableUserCoupons(
        @Schema(description = "사용자 ID")
        String userId
    );
}
