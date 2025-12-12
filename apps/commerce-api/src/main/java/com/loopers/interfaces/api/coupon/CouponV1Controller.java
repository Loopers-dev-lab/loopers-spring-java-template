package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponV1Controller implements CouponV1ApiSpec {
    private final CouponFacade couponFacade;

    @PostMapping
    @Override
    public ApiResponse<CouponV1Dto.CouponResponse> issueCoupon(@RequestBody CouponV1Dto.CouponRequest request) {
        CouponInfo info = couponFacade.issueCoupon(request);

        CouponV1Dto.CouponResponse response = CouponV1Dto.CouponResponse.from(info);
        return ApiResponse.success(response);
    }

    @GetMapping("/{userId}")
    @Override
    public ApiResponse<List<CouponV1Dto.CouponResponse>> findCoupons(@PathVariable Long userId) {
        List<CouponInfo> infos = couponFacade.findCoupons(userId);

        List<CouponV1Dto.CouponResponse> responses = infos.stream()
                .map(CouponV1Dto.CouponResponse::from)
                .toList();

        return ApiResponse.success(responses);
    }
}
