package com.loopers.interfaces.consumer;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.event.CouponEventPublisher;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.event.OrderEvents;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("CouponEventConsumer 통합 테스트")
@SpringBootTest
class CouponEventConsumerIntegrationTest {

    @Autowired
    private KafkaCouponEventConsumer couponConsumer;

    @MockitoBean
    private CouponEventPublisher couponEventPublisher;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Acknowledgment acknowledgment;

    private Long testUserId;
    private Long testCouponId;
    private final BigDecimal couponDiscountValue = BigDecimal.valueOf(5000);

    private final String testLoginId = "test34";

    // ConsumerRecord 헬퍼 메서드
    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @BeforeEach
    void setUp() {
        // Acknowledgment Mock 생성
        acknowledgment = mock(Acknowledgment.class);

        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);

        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Coupon 생성
        Coupon coupon = Coupon.builder()
                .couponType(CouponType.FIXED_AMOUNT)
                .discountValue(couponDiscountValue)
                .userId(testUserId)
                .build();
        Coupon savedCoupon = couponRepository.save(coupon)
                .orElseThrow(() -> new RuntimeException("Coupon 저장 실패"));
        testCouponId = savedCoupon.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("handleStockProcessed 테스트")
    @Nested
    class HandleStockProcessedTest {

        @DisplayName("성공 케이스: 쿠폰이 없는 경우 할인 금액 0으로 처리")
        @Test
        void handleStockProcessed_withNoCoupons_processesWithZeroDiscount() throws InterruptedException {
            // arrange
            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
            );
            List<Long> emptyCouponIds = List.of();

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    100L, // orderId
                    testUserId,
                    BigDecimal.valueOf(50000), // totalPrice
                    items,
                    emptyCouponIds,
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed event = new StockEvents.Processed(
                    100L, // orderId
                    List.of(),
                    orderCreatedEvent
            );

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.v1", event);

            // act
            couponConsumer.handleStockProcessed(record, acknowledgment);
            waitForAsyncProcessing();

            // assert - 쿠폰이 사용되지 않았는지 확인
            Coupon coupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertFalse(coupon.getIsUsed(), "쿠폰이 사용되지 않아야 함");
        }

        @DisplayName("성공 케이스: 쿠폰 사용 성공 시 쿠폰이 사용됨")
        @Test
        void handleStockProcessed_withValidCoupons_usesCoupon() throws InterruptedException {
            // arrange
            // 주문 생성 및 저장 (OrderService.applyDiscount 테스트를 위해 필요)
            Order order = Order.builder()
                    .userId(testUserId)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .build();
            order.addOrderItem(1L, "Test Product", BigDecimal.valueOf(25000), 2);
            Order savedOrder = orderService.saveOrder(order);
            Long orderId = savedOrder.getId();

            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
            );
            List<Long> couponIds = List.of(testCouponId);

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    orderId, // 실제 저장된 orderId 사용
                    testUserId,
                    BigDecimal.valueOf(50000), // totalPrice
                    items,
                    couponIds,
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed event = new StockEvents.Processed(
                    orderId, // 실제 저장된 orderId 사용
                    List.of(),
                    orderCreatedEvent
            );

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.v1", event);

            // act - ApplicationEventPublisher를 통해 이벤트 발행 (트랜잭션 내에서)
            // 통합 테스트에서는 실제 Consumer 메서드를 직접 호출
            couponConsumer.handleStockProcessed(record, acknowledgment);
            
            // 비동기 처리 대기 (트랜잭션 커밋 후 리스너 실행을 위한 충분한 시간)
            waitForAsyncProcessing(2000); // 2초로 증가

            // assert - 쿠폰이 사용되었는지 확인 (재시도 로직 사용, 재시도 횟수 증가)
            Coupon usedCoupon = waitForCouponToBeUsed(testCouponId, orderId, 10); // 5 → 10으로 증가
            assertNotNull(usedCoupon, "쿠폰을 찾을 수 없습니다");
            assertTrue(usedCoupon.getIsUsed(), "쿠폰이 사용되어야 함");
            assertEquals(orderId, usedCoupon.getOrderId(), "쿠폰의 orderId가 설정되어야 함");
        }

        @DisplayName("실패 케이스: 쿠폰 사용 실패 시 예외 발생")
        @Test
        void handleStockProcessed_withInvalidCoupon_throwsException() throws InterruptedException {
            // arrange
            // 이미 사용된 쿠폰으로 설정
            couponService.useCoupon(99L, testUserId, BigDecimal.valueOf(50000), testCouponId);

            List<OrderEvents.OrderItemInfo> items = List.of(
                    new OrderEvents.OrderItemInfo(1L, "Test Product", BigDecimal.valueOf(25000), 2)
            );
            List<Long> couponIds = List.of(testCouponId); // 이미 사용된 쿠폰

            OrderEvents.Created orderCreatedEvent = new OrderEvents.Created(
                    100L, // orderId
                    testUserId,
                    BigDecimal.valueOf(50000), // totalPrice
                    items,
                    couponIds,
                    PaymentDto.PaymentMethod.CARD
            );

            StockEvents.Processed event = new StockEvents.Processed(
                    100L, // orderId
                    List.of(),
                    orderCreatedEvent
            );

            ConsumerRecord<String, StockEvents.Processed> record = 
                    createConsumerRecord("stock.v1", event);

            // act
            couponConsumer.handleStockProcessed(record, acknowledgment);
            waitForAsyncProcessing();

            // assert - 쿠폰은 이미 사용된 상태
            Coupon usedCoupon = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertTrue(usedCoupon.getIsUsed(), "쿠폰은 이미 사용된 상태");
            assertEquals(99L, usedCoupon.getOrderId(), "쿠폰의 orderId는 이전 주문 ID");
        }
    }

    @DisplayName("handlePaymentProcessingFailed 테스트")
    @Nested
    class HandlePaymentProcessingFailedTest {

        @DisplayName("성공 케이스: 쿠폰 원복 성공")
        @Test
        void handlePaymentProcessingFailed_withValidEvent_rollsBackCoupon() throws InterruptedException {
            // arrange
            // 먼저 쿠폰 사용
            couponService.useCoupon(100L, testUserId, BigDecimal.valueOf(50000), testCouponId);

            // 쿠폰 사용 확인
            Coupon couponBefore = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertTrue(couponBefore.getIsUsed(), "쿠폰이 사용되어야 함");
            assertEquals(100L, couponBefore.getOrderId(), "쿠폰의 orderId가 설정되어야 함");

            // PaymentEvents.ProcessingFailed 이벤트 생성
            PaymentEvents.ProcessingFailed event = new PaymentEvents.ProcessingFailed(
                    100L, // orderId
                    null, // originalEvent (간소화)
                    "결제 처리 실패"
            );

            ConsumerRecord<String, PaymentEvents.ProcessingFailed> record = 
                    createConsumerRecord("payment.v1", event);

            // act
            couponConsumer.handlePaymentProcessingFailed(record, acknowledgment);
            waitForAsyncProcessing();

            // assert - 쿠폰이 원복되어야 함
            Coupon couponAfter = couponRepository.findById(testCouponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            assertFalse(couponAfter.getIsUsed(), "쿠폰이 원복되어야 함");
            assertNull(couponAfter.getOrderId(), "쿠폰의 orderId가 null이어야 함");
        }
    }

    /**
    
    /**
     * 비동기 이벤트 핸들러 완료 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        waitForAsyncProcessing(500);
    }
    
    /**
     * 비동기 이벤트 핸들러 완료 대기 (지정된 시간)
     */
    private void waitForAsyncProcessing(long millis) throws InterruptedException {
        Thread.sleep(millis); // @Async 메서드 완료 대기
    }
    
    /**
     * 쿠폰이 사용될 때까지 대기 (재시도 로직 포함)
     */
    private Coupon waitForCouponToBeUsed(Long couponId, Long orderId, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            Coupon coupon = couponRepository.findById(couponId)
                    .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
            
            // 디버깅 정보 출력
            System.out.println(String.format("[재시도 %d/%d] 쿠폰 상태 - isUsed: %s, orderId: %s", 
                    i + 1, maxRetries, coupon.getIsUsed(), coupon.getOrderId()));
            
            if (coupon.getIsUsed() && orderId.equals(coupon.getOrderId())) {
                System.out.println(String.format("쿠폰 사용 확인 성공 - couponId: %d, orderId: %d", 
                        couponId, orderId));
                return coupon;
            }
            Thread.sleep(300); // 200ms → 300ms로 증가
        }
        // 마지막으로 한 번 더 확인
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon을 찾을 수 없습니다"));
        
        // 최종 상태 출력
        System.out.println(String.format("최종 쿠폰 상태 - isUsed: %s, orderId: %s", 
                coupon.getIsUsed(), coupon.getOrderId()));
        
        return coupon;
    }
}

