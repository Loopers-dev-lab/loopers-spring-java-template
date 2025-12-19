package com.loopers.domain.coupon;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.order.OrderEvent;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 쿠폰 이벤트 핸들러 통합 테스트
 * 
 * 이벤트 기반 아키텍처에서:
 * - OrderService가 OrderCreatedEvent를 발행 (주문 생성 후)
 * - CouponService가 이벤트를 받아 쿠폰 사용 처리 (AFTER_COMMIT, 비동기)
 * - 주문 생성 실패 시 쿠폰이 사용되지 않아야 함
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("쿠폰 이벤트 핸들러 통합 테스트")
public class CouponEventHandlerIntegrationTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long userId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        // User 등록
        User user = userService.registerUser("user123", "test@test.com", "1993-03-13", "male");
        userId = user.getId();

        // Coupon 발급
        Coupon coupon = couponService.issueFixed(userId, 5000);
        couponId = coupon.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("OrderCreatedEvent를 받아 쿠폰 사용 처리할 때")
    @Nested
    class HandleOrderCreated {
        @DisplayName("쿠폰이 있는 경우 쿠폰을 사용 처리할 수 있다")
        @Test
        void should_markCouponAsUsed_when_couponExists() throws InterruptedException {
            // arrange
            Price originalPrice = new Price(10000);
            DiscountResult discountResult = couponService.calculateDiscount(couponId, userId, originalPrice);
            assertThat(discountResult.finalPrice().amount()).isEqualTo(5000);

            // 쿠폰이 아직 사용되지 않았는지 확인
            Coupon couponBefore = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(couponBefore.isUsed()).isFalse();

            // act - 이벤트 발행 (주문 생성 시뮬레이션)
            OrderEvent.OrderCreatedEvent event = OrderEvent.createOrderCreatedEvent(
                    1L, // orderId
                    "order-123", // orderPublicId
                    userId,
                    couponId,
                    discountResult.finalPrice(),
                    com.loopers.domain.order.PaymentMethod.POINT
            );

            eventPublisher.publishEvent(event);

            // assert - 비동기 처리 대기
            Thread.sleep(2000);

            Coupon couponAfter = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(couponAfter.isUsed()).isTrue();
        }

        @DisplayName("쿠폰이 없는 경우 아무것도 하지 않는다")
        @Test
        void should_doNothing_when_couponIdIsNull() throws InterruptedException {
            // arrange
            OrderEvent.OrderCreatedEvent event = OrderEvent.createOrderCreatedEvent(
                    1L,
                    "order-123",
                    userId,
                    null, // 쿠폰 없음
                    new Price(10000),
                    com.loopers.domain.order.PaymentMethod.POINT
            );

            // act
            eventPublisher.publishEvent(event);
            
            // assert - 비동기 처리 대기 (폴링 방식)
            long deadline = System.currentTimeMillis() + 2000; // 최대 2초 대기
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            
            // assert - 예외가 발생하지 않아야 함
            Coupon coupon = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(coupon.isUsed()).isFalse(); // 여전히 사용되지 않음
        }

        @DisplayName("이미 사용된 쿠폰인 경우 예외가 발생하지만 주문에는 영향이 없다")
        @Test
        void should_notAffectOrder_when_couponAlreadyUsed() throws InterruptedException {
            // arrange
            // 이미 사용된 쿠폰으로 설정
            Coupon coupon = couponJpaRepository.findById(couponId).orElseThrow();
            coupon.markAsUsed();
            couponJpaRepository.save(coupon);

            // 이미 사용된 쿠폰으로 이벤트 발행 (주문은 이미 생성되었다고 가정)
            // 이벤트 핸들러에서 이미 사용된 쿠폰을 처리하려고 시도하지만 예외가 발생하고 로그만 남김
            OrderEvent.OrderCreatedEvent event = OrderEvent.createOrderCreatedEvent(
                    1L,
                    "order-123",
                    userId,
                    couponId,
                    new Price(5000), // 할인 후 금액 (이미 계산된 것으로 가정)
                    com.loopers.domain.order.PaymentMethod.POINT
            );

            // act - 이벤트 발행 (이벤트 핸들러에서 예외가 발생하지만 주문에는 영향 없음)
            eventPublisher.publishEvent(event);
            
            // assert - 비동기 처리 대기 (폴링 방식)
            long deadline = System.currentTimeMillis() + 2000; // 최대 2초 대기
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            
            // assert - 이벤트 핸들러에서 예외가 발생하지만 로그만 남기고 주문에는 영향 없음
            // 쿠폰은 여전히 사용된 상태로 유지됨
            Coupon couponAfter = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(couponAfter.isUsed()).isTrue(); // 여전히 사용된 상태
        }
    }

    @DisplayName("실제 주문 생성 플로우에서 쿠폰 사용 처리")
    @Nested
    class OrderCreationFlow {
        @DisplayName("주문 생성 시 쿠폰이 비동기로 사용 처리된다")
        @Test
        void should_markCouponAsUsed_async_when_orderCreated() throws InterruptedException {
            // arrange
            // 쿠폰이 아직 사용되지 않았는지 확인
            Coupon couponBefore = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(couponBefore.isUsed()).isFalse();

            // act - 주문 생성 이벤트 발행 (실제 주문 생성은 OrderFacadeIntegrationTest에서 테스트)
            OrderEvent.OrderCreatedEvent event = OrderEvent.createOrderCreatedEvent(
                    1L,
                    "order-123",
                    userId,
                    couponId,
                    new Price(5000), // 할인 후 금액
                    com.loopers.domain.order.PaymentMethod.POINT
            );

            eventPublisher.publishEvent(event);

            // assert - 비동기 처리 대기 (폴링 방식)
            long deadline = System.currentTimeMillis() + 5000; // 최대 5초 대기
            while (System.currentTimeMillis() < deadline) {
                Coupon coupon = couponJpaRepository.findById(couponId).orElseThrow();
                if (coupon.isUsed()) {
                    break;
                }
                Thread.sleep(50);
            }
            Coupon couponAfter = couponJpaRepository.findById(couponId).orElseThrow();
            assertThat(couponAfter.isUsed()).isTrue();
        }
    }
}

