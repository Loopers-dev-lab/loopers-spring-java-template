package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CouponFacade {
    
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    
    public CouponFacade(CouponRepository couponRepository, CouponService couponService) {
        this.couponRepository = couponRepository;
        this.couponService = couponService;
    }
    
    public CouponCommand.CouponResponse issueFixedCoupon(CouponCommand.CreateFixedCouponRequest request) {
        CouponModel coupon = CouponModel.createFixed(
                request.userId(),
                request.amount(),
                request.expiredAt()
        );
        
        CouponModel savedCoupon = couponRepository.save(coupon);
        return CouponCommand.CouponResponse.from(savedCoupon);
    }
    
    public CouponCommand.CouponResponse issueRateCoupon(CouponCommand.CreateRateCouponRequest request) {
        CouponModel coupon = CouponModel.createRate(
                request.userId(),
                request.rate(),
                request.expiredAt()
        );
        
        CouponModel savedCoupon = couponRepository.save(coupon);
        return CouponCommand.CouponResponse.from(savedCoupon);
    }
    
    public CouponCommand.DiscountCalculationResponse useCoupon(CouponCommand.UseCouponRequest request) {
        CouponModel coupon = couponRepository.findById(request.couponId())
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 찾을 수 없습니다."));
        
        BigDecimal discountAmount = coupon.calculateDiscountAmount(request.orderAmount());
        BigDecimal finalAmount = request.orderAmount().subtract(discountAmount);
        
        coupon.use(request.orderId());
        couponRepository.save(coupon);
        
        return new CouponCommand.DiscountCalculationResponse(
                request.orderAmount(),
                discountAmount,
                finalAmount,
                CouponCommand.CouponResponse.from(coupon)
        );
    }
    
    public List<CouponCommand.CouponResponse> getUserCoupons(Long userId) {
        List<CouponModel> coupons = couponRepository.findByUserId(userId);
        return CouponCommand.CouponResponse.fromList(coupons);
    }
    
    public List<CouponCommand.CouponResponse> getUserUsableCoupons(Long userId) {
        List<CouponModel> coupons = couponRepository.findUsableCouponsByUserId(userId);
        return CouponCommand.CouponResponse.fromList(coupons);
    }
    
    public CouponCommand.CouponResponse findBestCouponForUser(Long userId, BigDecimal orderAmount) {
        List<CouponModel> usableCoupons = couponRepository.findUsableCouponsByUserId(userId);
        
        if (usableCoupons.isEmpty()) {
            return null;
        }
        
        CouponModel bestCoupon = couponService.selectBestCoupon(usableCoupons, orderAmount);
        return CouponCommand.CouponResponse.from(bestCoupon);
    }
    
    public CouponCommand.DiscountCalculationResponse calculateDiscount(Long couponId, BigDecimal orderAmount) {
        CouponModel coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST, "쿠폰을 찾을 수 없습니다."));
        
        return CouponCommand.DiscountCalculationResponse.of(orderAmount, coupon);
    }
    
    public List<CouponCommand.CouponResponse> getCouponsByOrderId(Long orderId) {
        List<CouponModel> coupons = couponRepository.findByOrderId(orderId);
        return CouponCommand.CouponResponse.fromList(coupons);
    }

    @Transactional
    public CouponModel useCoupon(Long couponId, Long orderId) {
        CouponModel coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        coupon.use(orderId);

        return couponRepository.save(coupon);
    }
}
