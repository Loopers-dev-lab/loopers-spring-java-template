package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }
    public CouponModel getUserCoupons(Long couponId, Long userId) {
        return couponRepository.findByIdAndUserId(couponId, userId).orElseThrow(
                () -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 찾을 수 없습니다.")
        );
    }
    public List<CouponModel> filterCouponsByUser(List<CouponModel> coupons, Long userId) {
        return coupons.stream()
                .filter(coupon -> coupon.belongsToUser(userId))
                .toList();
    }

    public List<CouponModel> filterUsableCoupons(List<CouponModel> coupons) {
        return coupons.stream()
                .filter(CouponModel::canUse)
                .toList();
    }

    public CouponModel selectBestCoupon(List<CouponModel> coupons, BigDecimal orderAmount) {
        if (coupons.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 있는 쿠폰이 없습니다.");
        }

        return coupons.stream()
                .max(Comparator.comparing(coupon -> coupon.calculateDiscountAmount(orderAmount)))
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "사용할 수 있는 쿠폰이 없습니다."));
    }

    public CouponModel findBestCouponForUser(List<CouponModel> allCoupons, Long userId, BigDecimal orderAmount) {
        List<CouponModel> userCoupons = filterCouponsByUser(allCoupons, userId);
        List<CouponModel> usableCoupons = filterUsableCoupons(userCoupons);
        return selectBestCoupon(usableCoupons, orderAmount);
    }

    public BigDecimal calculateTotalDiscount(List<CouponModel> coupons, BigDecimal orderAmount) {
        return coupons.stream()
                .filter(CouponModel::canUse)
                .map(coupon -> coupon.calculateDiscountAmount(orderAmount))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CouponCommand.Result applyCouponToOrder(CouponModel coupon, BigDecimal orderAmount) {
        if (!coupon.canUse()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }

        BigDecimal discountAmount = coupon.calculateDiscountAmount(orderAmount);
        
        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        return new CouponCommand.Result(coupon, discountAmount);
    }


}
