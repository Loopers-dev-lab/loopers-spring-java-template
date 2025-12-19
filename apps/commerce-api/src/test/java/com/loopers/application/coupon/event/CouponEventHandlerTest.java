package com.loopers.application.coupon.event;

import com.loopers.domain.coupon.CouponDomainService;
import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponEventHandlerTest {

    @Mock
    private CouponDomainService couponDomainService;

    @InjectMocks
    private CouponEventHandler couponEventHandler;

    @Test
    @DisplayName("쿠폰 사용 이벤트를 받으면 쿠폰을 사용 처리한다")
    void handleCouponUsedTest1() {
        // arrange
        Long couponId = 1L;
        Long orderId = 100L;
        String userId = "user-1";
        long discountAmount = 1000L;

        CouponUsedEvent event = CouponUsedEvent.from(couponId, orderId, userId, discountAmount);

        // act
        couponEventHandler.handleCouponUsed(event);

        // assert
        verify(couponDomainService, times(1)).useCoupon(couponId);
    }

    @Test
    @DisplayName("쿠폰 사용 처리 중 예외가 발생해도 이벤트 핸들러는 예외를 던지지 않는다")
    void handleCouponUsedTest2() {
        // arrange
        Long couponId = 999L;
        Long orderId = 100L;
        String userId = "user-1";
        long discountAmount = 1000L;

        CouponUsedEvent event = CouponUsedEvent.from(couponId, orderId, userId, discountAmount);
        doThrow(new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다"))
                .when(couponDomainService).useCoupon(couponId);

        // act - 예외 없이 정상 종료
        couponEventHandler.handleCouponUsed(event);

        // assert
        verify(couponDomainService, times(1)).useCoupon(couponId);
    }

    @Test
    @DisplayName("이벤트의 couponId가 올바르게 전달된다")
    void handleCouponUsedTest3() {
        // arrange
        Long couponId = 12345L;
        Long orderId = 500L;
        String userId = "specific-user";
        long discountAmount = 5000L;

        CouponUsedEvent event = CouponUsedEvent.from(couponId, orderId, userId, discountAmount);

        // act
        couponEventHandler.handleCouponUsed(event);

        // assert
        verify(couponDomainService).useCoupon(couponId);
    }
}
