package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.domain.coupon.Coupon;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupons")
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponFacade couponFacade;

    @PostMapping
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> issueCoupon(
            @RequestHeader(value = "X-USER-ID") String userId,
            @Valid @RequestBody CouponV1Dto.IssueRequest request
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        Coupon coupon = couponFacade.issueCoupon(
                userId,
                request.couponName(),
                request.couponType(),
                request.discountValue(),
                request.expiresAt()
        );

        return ApiResponse.success(CouponV1Dto.CouponResponse.from(coupon));
    }

    @PostMapping("/{couponId}/use")
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> useCoupon(
            @RequestHeader(value = "X-USER-ID") String userId,
            @PathVariable Long couponId
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        Coupon coupon = couponFacade.useCoupon(couponId, userId);
        return ApiResponse.success(CouponV1Dto.CouponResponse.from(coupon));
    }

    @GetMapping("/{couponId}")
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> getCoupon(
            @PathVariable Long couponId
    ) {
        Coupon coupon = couponFacade.getCoupon(couponId);
        return ApiResponse.success(CouponV1Dto.CouponResponse.from(coupon));
    }

    @GetMapping("/my")
    @Override
    public ApiResponse<CouponV1Dto.CouponListResponse> getMyCoupons(
            @RequestHeader(value = "X-USER-ID") String userId
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        List<Coupon> coupons = couponFacade.getUserCoupons(userId);
        return ApiResponse.success(CouponV1Dto.CouponListResponse.from(coupons));
    }

    @GetMapping("/available")
    @Override
    public ApiResponse<CouponV1Dto.CouponListResponse> getAvailableCoupons(
            @RequestHeader(value = "X-USER-ID") String userId
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "필수 요청 헤더 'X-USER-ID'는 빈 값일 수 없습니다.");
        }

        List<Coupon> coupons = couponFacade.getAvailableCoupons(userId);
        return ApiResponse.success(CouponV1Dto.CouponListResponse.from(coupons));
    }

    @PostMapping("/{couponId}/calculate-discount")
    @Override
    public ApiResponse<CouponV1Dto.CalculateDiscountResponse> calculateDiscount(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponV1Dto.CalculateDiscountRequest request
    ) {
        BigDecimal discountAmount = couponFacade.calculateDiscount(couponId, request.originalAmount());
        return ApiResponse.success(CouponV1Dto.CalculateDiscountResponse.from(discountAmount));
    }
}
