package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.coupon.CouponV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponFacade {
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public CouponInfo issueCoupon(CouponV1Dto.CouponRequest request) {
        /*
        👨‍💻 쿠폰 발급 로직
        - [ ] 사용자 검증
        - [ ] 쿠폰 발급
         */
        Long userId = request.userId();
        userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")
        );

        Coupon coupon = Coupon.create(request);
        Coupon saved = couponRepository.save(coupon);

        return CouponInfo.from(saved);
    }

    public List<CouponInfo> findCoupons(Long userId) {
        /*
        👨‍💻 사용 가능한 쿠폰 조회 로직
        - [ ] 사용자 검증
        - [ ] 쿠폰 조회
         */
        userRepository.findById(userId).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 회원입니다.")
        );

        List<Coupon> coupons = couponRepository.findAllByUserId(userId).orElseThrow(
                () -> new CoreException(ErrorType.BAD_REQUEST, "사용 가능한 쿠폰이 존재하지 않습니다.")
        );

        return coupons.stream()
                .map(CouponInfo::from)
                .toList();
    }
}
