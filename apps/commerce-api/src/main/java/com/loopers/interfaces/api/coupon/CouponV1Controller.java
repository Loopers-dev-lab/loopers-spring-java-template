package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponService;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponService couponService;

    @PostMapping
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> createCoupon(
        @Valid @RequestBody CouponV1Dto.CreateCouponRequest request
    ) {
        Coupon coupon = Coupon.builder()
            .name(request.name())
            .type(request.type())
            .discountValue(request.discountValue())
            .description(request.description())
            .build();

        Coupon createdCoupon = couponService.createCoupon(coupon);
        CouponV1Dto.CouponResponse response = CouponV1Dto.CouponResponse.from(createdCoupon);
        return ApiResponse.success(response);
    }

    @GetMapping("/{couponId}")
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> getCoupon(
        @PathVariable Long couponId
    ) {
        Coupon coupon = couponService.getCoupon(couponId);
        CouponV1Dto.CouponResponse response = CouponV1Dto.CouponResponse.from(coupon);
        return ApiResponse.success(response);
    }

    @GetMapping
    @Override
    public ApiResponse<List<CouponV1Dto.CouponResponse>> getAllCoupons() {
        List<Coupon> coupons = couponService.getAllCoupons();
        List<CouponV1Dto.CouponResponse> response = coupons.stream()
            .map(CouponV1Dto.CouponResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping("/users/{userId}/issue/{couponId}")
    @Override
    public ApiResponse<CouponV1Dto.UserCouponResponse> issueCouponToUser(
        @PathVariable String userId,
        @PathVariable Long couponId
    ) {
        UserCoupon userCoupon = couponService.issueCouponToUser(userId, couponId);
        CouponV1Dto.UserCouponResponse response = CouponV1Dto.UserCouponResponse.from(
            userCoupon);
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}")
    @Override
    public ApiResponse<List<CouponV1Dto.UserCouponResponse>> getUserCoupons(
        @PathVariable String userId
    ) {
        List<UserCoupon> userCoupons = couponService.getUserCoupons(userId);
        List<CouponV1Dto.UserCouponResponse> response = userCoupons.stream()
            .map(CouponV1Dto.UserCouponResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/users/{userId}/available")
    @Override
    public ApiResponse<List<CouponV1Dto.UserCouponResponse>> getAvailableUserCoupons(
        @PathVariable String userId
    ) {
        List<UserCoupon> userCoupons = couponService.getAvailableUserCoupons(userId);
        List<CouponV1Dto.UserCouponResponse> response = userCoupons.stream()
            .map(CouponV1Dto.UserCouponResponse::from)
            .toList();
        return ApiResponse.success(response);
    }
}
