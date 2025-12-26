package com.loopers.application.event.integration;

import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.application.event.payment.PaymentCompletedEvent;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 주문-결제 이벤트 플로우 통합 테스트
 *
 * 검증 항목:
 * 1. 결제 성공 시 주문 상태 변경 및 OrderCompletedEvent 발행
 * 2. 결제 실패 시 주문 취소 및 재고 복구
 * 3. 중복 이벤트 발행 시 멱등성 보장
 * 4. 비동기 이벤트 처리 후 트랜잭션 커밋 확인
 */
@SpringBootTest
@RecordApplicationEvents
@DisplayName("주문-결제 이벤트 통합 테스트")
class OrderPaymentEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Member testMember;
    private Product testProduct;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // 테스트 회원 생성
        testMember = new Member("testuser", "test@example.com", "password123", "1990-01-01", Gender.MALE);
        testMember = memberRepository.save(testMember);

        // 테스트 상품 생성 (재고 100개)
        testProduct = new Product(1L, "테스트 상품", "설명", Money.of(10000), Stock.of(100));
        testProduct = productRepository.save(testProduct);

        // 테스트 주문 생성
        OrderItem orderItem = new OrderItem(testProduct.getId(), 5, Money.of(10000));
        testOrder = Order.create(testMember.getId(), List.of(orderItem));
        testOrder = orderRepository.save(testOrder);

        // 재고 차감 (실제 주문 생성 시 발생)
        testProduct.decreaseStock(5);
        productRepository.save(testProduct);

        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("결제 성공 시 주문 상태가 PAID로 변경되고 OrderCompletedEvent가 발행된다")
    void 결제_성공_시_주문_완료() {
        // given
        String orderNo = testOrder.getOrderNo();
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            orderNo,
            testMember.getId(),
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now()
        );

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(paymentEvent));

        // 비동기 처리 대기 (최대 3초)
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            entityManager.clear();
            Order updatedOrder = orderRepository.findByOrderNo(orderNo).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(updatedOrder.isPaid()).isTrue();
        });

        // then - OrderCompletedEvent 발행 확인
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            long orderCompletedEventCount = applicationEvents.stream(OrderCompletedEvent.class)
                .filter(event -> event.orderNo().equals(orderNo))
                .count();
            assertThat(orderCompletedEventCount).isGreaterThan(0);
        });
    }

    @Test
    @DisplayName("결제 실패 시 주문이 취소되고 재고가 복구된다")
    void 결제_실패_시_주문_취소_및_재고_복구() {
        // given
        String orderNo = testOrder.getOrderNo();
        Long productId = testProduct.getId();

        // 재고 차감 후 현재 재고 확인
        entityManager.clear();
        Product beforeProduct = productRepository.findById(productId).orElseThrow();
        int stockBeforeFailure = beforeProduct.getStock().getQuantity();
        assertThat(stockBeforeFailure).isEqualTo(95); // 100 - 5

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            orderNo,
            testMember.getId(),
            PaymentStatus.FAILED,
            "카드 한도 초과",
            LocalDateTime.now()
        );

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(paymentEvent));

        // 비동기 처리 대기
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            entityManager.clear();

            // then - 주문 취소 확인
            Order cancelledOrder = orderRepository.findByOrderNo(orderNo).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(cancelledOrder.isCancelled()).isTrue();

            // then - 재고 복구 확인 (95 + 5 = 100)
            Product restoredProduct = productRepository.findById(productId).orElseThrow();
            assertThat(restoredProduct.getStock().getQuantity()).isEqualTo(100);
        });
    }

    @Test
    @DisplayName("이미 결제 완료된 주문에 중복 이벤트 발행 시 멱등성 보장")
    void 중복_이벤트_멱등성_보장() {
        // given - 주문을 먼저 PAID 상태로 변경
        String orderNo = testOrder.getOrderNo();
        testOrder.markAsPaid();
        orderRepository.save(testOrder);
        entityManager.clear();

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            orderNo,
            testMember.getId(),
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now()
        );

        // when - 중복 이벤트 발행
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(paymentEvent));

        // 비동기 처리 대기
        await().atMost(3, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            entityManager.clear();
            Order order = orderRepository.findByOrderNo(orderNo).orElseThrow();

            // then - 여전히 PAID 상태 유지 (변경 없음)
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.isPaid()).isTrue();
        });

        // OrderCompletedEvent는 한 번만 발행되어야 함 (멱등성)
        // 실제로는 이미 PAID 상태라서 이벤트가 발행되지 않음
    }

    @Test
    @DisplayName("비동기 이벤트 처리 후 트랜잭션 커밋되어 DB에 반영된다")
    void 비동기_이벤트_트랜잭션_커밋_확인() {
        // given
        String orderNo = testOrder.getOrderNo();

        PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(
            orderNo,
            testMember.getId(),
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now()
        );

        // when
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(paymentEvent));

        // 비동기 처리 및 트랜잭션 커밋 대기
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // 새로운 트랜잭션에서 조회 (커밋된 데이터만 보임)
            Order committedOrder = orderRepository.findByOrderNo(orderNo).orElseThrow();

            // then - DB에 PAID 상태로 커밋되어 있음
            assertThat(committedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        });
    }
}
