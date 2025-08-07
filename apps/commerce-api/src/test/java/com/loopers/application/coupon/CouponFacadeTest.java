package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("CouponFacade 테스트")
class CouponFacadeTest {

    @Mock
    private CouponRepository couponRepository;
    
    @Mock
    private CouponService couponService;
    
    private CouponFacade couponFacade;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        couponFacade = new CouponFacade(couponRepository, couponService);
    }
    
    @Test
    @DisplayName("정액 할인 쿠폰을 발급할 수 있다")
    void issueFixedCoupon() {
        // Arrange
        CouponCommand.CreateFixedCouponRequest request = new CouponCommand.CreateFixedCouponRequest(
                1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        
        CouponModel savedCoupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        when(couponRepository.save(any(CouponModel.class))).thenReturn(savedCoupon);
        
        // Act
        CouponCommand.CouponResponse response = couponFacade.issueFixedCoupon(request);
        
        // Assert
        assertThat(response.type()).isEqualTo("FIXED");
        assertThat(response.value()).isEqualTo(new BigDecimal("5000"));
        assertThat(response.userId()).isEqualTo(1L);
        verify(couponRepository).save(any(CouponModel.class));
    }
    
    @Test
    @DisplayName("정률 할인 쿠폰을 발급할 수 있다")
    void issueRateCoupon() {
        // Arrange
        CouponCommand.CreateRateCouponRequest request = new CouponCommand.CreateRateCouponRequest(
                1L, new BigDecimal("0.1"), LocalDateTime.now().plusDays(30));
        
        CouponModel savedCoupon = CouponModel.createRate(1L, new BigDecimal("0.1"), LocalDateTime.now().plusDays(30));
        when(couponRepository.save(any(CouponModel.class))).thenReturn(savedCoupon);
        
        // Act
        CouponCommand.CouponResponse response = couponFacade.issueRateCoupon(request);
        
        // Assert
        assertThat(response.type()).isEqualTo("RATE");
        assertThat(response.value()).isEqualTo(new BigDecimal("0.1"));
        assertThat(response.userId()).isEqualTo(1L);
        verify(couponRepository).save(any(CouponModel.class));
    }
    
    @Test
    @DisplayName("쿠폰을 사용할 수 있다")
    void useCoupon() {
        // Arrange
        Long couponId = 1L;
        Long orderId = 100L;
        BigDecimal orderAmount = new BigDecimal("10000");
        
        CouponModel coupon = CouponModel.createFixed(1L, new BigDecimal("5000"), LocalDateTime.now().plusDays(30));
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any(CouponModel.class))).thenReturn(coupon);
        
        CouponCommand.UseCouponRequest request = new CouponCommand.UseCouponRequest(couponId, orderId, orderAmount);
        
        // Act
        CouponCommand.DiscountCalculationResponse response = couponFacade.useCoupon(request);
        
        // Assert
        assertThat(response.originalAmount()).isEqualTo(orderAmount);
        assertThat(response.discountAmount()).isEqualTo(new BigDecimal("5000"));
        assertThat(response.finalAmount()).isEqualTo(new BigDecimal("5000"));
        verify(couponRepository).findById(couponId);
        verify(couponRepository).save(any(CouponModel.class));
    }
    
    @Test
    @DisplayName("존재하지 않는 쿠폰을 사용하려고 하면 예외가 발생한다")
    void throwExceptionWhenCouponNotExists() {
        // Arrange
        Long couponId = 999L;
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());
        
        CouponCommand.UseCouponRequest request = new CouponCommand.UseCouponRequest(
                couponId, 100L, new BigDecimal("10000"));
        
        // Act & Assert
        assertThatThrownBy(() -> couponFacade.useCoupon(request))
                .isInstanceOf(CoreException.class)
                .hasMessage("쿠폰을 찾을 수 없습니다.");
    }
    
    @Test
    @DisplayName("사용자의 쿠폰 목록을 조회할 수 있다")
    void getUserCoupons() {
        // Arrange
        Long userId = 1L;
        List<CouponModel> coupons = List.of(
                CouponModel.createFixed(userId, new BigDecimal("5000"), LocalDateTime.now().plusDays(30)),
                CouponModel.createRate(userId, new BigDecimal("0.1"), LocalDateTime.now().plusDays(30))
        );
        when(couponRepository.findByUserId(userId)).thenReturn(coupons);
        
        // Act
        List<CouponCommand.CouponResponse> responses = couponFacade.getUserCoupons(userId);
        
        // Assert
        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(response -> response.userId().equals(userId));
        verify(couponRepository).findByUserId(userId);
    }
    
    @Test
    @DisplayName("사용자의 사용 가능한 쿠폰 중 최적의 쿠폰을 찾을 수 있다")
    void findBestCouponForUser() {
        // Arrange
        Long userId = 1L;
        BigDecimal orderAmount = new BigDecimal("10000");
        
        List<CouponModel> usableCoupons = List.of(
                CouponModel.createFixed(userId, new BigDecimal("3000"), LocalDateTime.now().plusDays(30)),
                CouponModel.createFixed(userId, new BigDecimal("5000"), LocalDateTime.now().plusDays(30))
        );
        
        when(couponRepository.findUsableCouponsByUserId(userId)).thenReturn(usableCoupons);
        when(couponService.selectBestCoupon(usableCoupons, orderAmount))
                .thenReturn(usableCoupons.get(1)); // 5000원 쿠폰이 더 좋음
        
        // Act
        CouponCommand.CouponResponse response = couponFacade.findBestCouponForUser(userId, orderAmount);
        
        // Assert
        assertThat(response.value()).isEqualTo(new BigDecimal("5000"));
        verify(couponRepository).findUsableCouponsByUserId(userId);
        verify(couponService).selectBestCoupon(usableCoupons, orderAmount);
    }
    
    @Test
    @DisplayName("사용 가능한 쿠폰이 없으면 null을 반환한다")
    void returnNullWhenNoUsableCoupons() {
        // Arrange
        Long userId = 1L;
        BigDecimal orderAmount = new BigDecimal("10000");
        
        when(couponRepository.findUsableCouponsByUserId(userId)).thenReturn(List.of());
        
        // Act
        CouponCommand.CouponResponse response = couponFacade.findBestCouponForUser(userId, orderAmount);
        
        // Assert
        assertThat(response).isNull();
        verify(couponRepository).findUsableCouponsByUserId(userId);
        verify(couponService, never()).selectBestCoupon(any(), any());
    }
}