package com.loopers.application.payment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.quantity.Quantity;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.interfaces.api.payment.PaymentCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.test.IntegrationTestSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("PaymentCallbackFacade 통합 테스트")
class PaymentCallbackFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  private static final LocalDateTime ORDERED_AT_2025_12_01 = LocalDateTime.of(2025, 12, 1, 10, 0, 0);
  private static final LocalDateTime REQUESTED_AT_2025_12_01 = LocalDateTime.of(2025, 12, 1, 10, 5, 0);
  private static final String TRANSACTION_KEY = "TXN_TEST_001";
  private static final String CARD_NO = "1234-5678-9012-3456";

  @Autowired
  private PaymentCallbackFacade paymentCallbackFacade;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private OrderJpaRepository orderJpaRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PointRepository pointRepository;

  @Autowired
  private BrandJpaRepository brandJpaRepository;

  private User user;
  private Brand brand;
  private Product product;
  private Order order;
  private Payment payment;

  @BeforeEach
  void setUp() {
    user = userRepository.save(
        User.of("testuser", "test@example.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 12, 1))
    );

    brand = brandJpaRepository.save(Brand.of("테스트브랜드", "테스트 브랜드 설명"));

    product = productRepository.save(
        Product.of("테스트상품", Money.of(10000L), "상품 설명", Stock.of(10L), brand.getId())
    );

    pointRepository.save(Point.of(user.getId(), 100000L));

    order = Order.of(user.getId(), OrderStatus.PENDING, 30000L, 20000L, 10000L, ORDERED_AT_2025_12_01);
    addOrderItem(order, product.getId(), "테스트상품", 3L, 10000L);
    order = orderJpaRepository.save(order);

    payment = Payment.of(order.getId(), user.getId(), CardType.SAMSUNG, CARD_NO, 10000L, REQUESTED_AT_2025_12_01);
    payment.toPending(TRANSACTION_KEY);
    payment = paymentRepository.save(payment);
  }

  private void addOrderItem(Order order, Long productId, String productName, Long quantity, Long price) {
    OrderItem item = OrderItem.of(productId, productName, Quantity.of(quantity), OrderPrice.of(price));
    order.addItem(item);
  }

  @Nested
  @DisplayName("handleCallback")
  class HandleCallback {

    @Test
    @DisplayName("SUCCESS 콜백이면 결제를 성공 처리한다")
    void shouldProcessSuccess_whenSuccessCallback() {
      // given
      PaymentCallbackRequest request = new PaymentCallbackRequest(
          TRANSACTION_KEY, order.getId().toString(), "SAMSUNG", CARD_NO, 10000L, "SUCCESS", null
      );

      // when
      paymentCallbackFacade.handleCallback(request);

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(order.getId()).orElseThrow();
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment).extracting("status").isEqualTo(PaymentStatus.SUCCESS),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.COMPLETED),
            () -> assertThat(updatedProduct.getStockValue()).isEqualTo(7L)
        );
      });
    }

    @Test
    @DisplayName("FAILED 콜백이면 결제를 실패 처리한다")
    void shouldProcessFailed_whenFailedCallback() {
      // given
      PaymentCallbackRequest request = new PaymentCallbackRequest(
          TRANSACTION_KEY, order.getId().toString(), "SAMSUNG", CARD_NO, 10000L, "FAILED", "카드 한도 초과"
      );

      // when
      paymentCallbackFacade.handleCallback(request);

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(order.getId()).orElseThrow();
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment)
                .extracting("status", "failureReason")
                .containsExactly(PaymentStatus.FAILED, "카드 한도 초과"),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED),
            () -> assertThat(updatedPoint.getAmountValue()).isEqualTo(120000L)
        );
      });
    }

    @Test
    @DisplayName("알 수 없는 상태면 아무 처리도 하지 않는다")
    void shouldDoNothing_whenUnknownStatus() {
      // given
      PaymentCallbackRequest request = new PaymentCallbackRequest(
          TRANSACTION_KEY, order.getId().toString(), "SAMSUNG", CARD_NO, 10000L, "UNKNOWN", null
      );

      // when
      paymentCallbackFacade.handleCallback(request);

      // then
      Payment unchangedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
      Order unchangedOrder = orderJpaRepository.findById(order.getId()).orElseThrow();

      assertAll(
          () -> assertThat(unchangedPayment).extracting("status").isEqualTo(PaymentStatus.PENDING),
          () -> assertThat(unchangedOrder).extracting("status").isEqualTo(OrderStatus.PENDING)
      );
    }
  }

  @Nested
  @DisplayName("handleSuccess")
  class HandleSuccess {

    @Test
    @DisplayName("정상 콜백이면 결제 성공, 주문 완료, 재고 차감이 수행된다")
    void shouldCompletePaymentAndOrder_whenSuccess() {
      // given - 기본 픽스처 사용

      // when
      paymentCallbackFacade.handleSuccess(TRANSACTION_KEY);

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(order.getId()).orElseThrow();
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment).extracting("status").isEqualTo(PaymentStatus.SUCCESS),
            () -> assertThat(updatedPayment.getPgCompletedAt()).isNotNull(),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.COMPLETED),
            () -> assertThat(updatedProduct.getStockValue()).isEqualTo(7L)
        );
      });
    }

    @Test
    @DisplayName("재고 부족이면 결제는 SUCCESS이지만 주문은 PAYMENT_FAILED가 되고 포인트가 환불된다")
    void shouldRefundPoint_whenStockInsufficient() {
      // given
      Product lowStockProduct = productRepository.save(
          Product.of("재고부족상품", Money.of(10000L), "설명", Stock.of(1L), brand.getId())
      );

      Order lowStockOrder = Order.of(user.getId(), OrderStatus.PENDING, 50000L, 30000L, 20000L, ORDERED_AT_2025_12_01);
      addOrderItem(lowStockOrder, lowStockProduct.getId(), "재고부족상품", 5L, 10000L);
      Order savedLowStockOrder = orderJpaRepository.save(lowStockOrder);

      Payment lowStockPayment = Payment.of(
          savedLowStockOrder.getId(), user.getId(), CardType.KB, CARD_NO, 20000L, REQUESTED_AT_2025_12_01
      );
      lowStockPayment.toPending("TXN_LOW_STOCK");
      paymentRepository.save(lowStockPayment);

      // when
      paymentCallbackFacade.handleSuccess("TXN_LOW_STOCK");

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey("TXN_LOW_STOCK").orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(savedLowStockOrder.getId()).orElseThrow();
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment).extracting("status").isEqualTo(PaymentStatus.SUCCESS),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED),
            () -> assertThat(updatedPoint.getAmountValue()).isEqualTo(130000L)
        );
      });
    }

    @Test
    @DisplayName("이미 완료된 결제면 아무 처리도 하지 않는다")
    void shouldDoNothing_whenAlreadyCompleted() {
      // given
      payment.toSuccess(REQUESTED_AT_2025_12_01.plusMinutes(1));
      paymentRepository.save(payment);

      Long originalStock = productRepository.findById(product.getId()).orElseThrow().getStockValue();

      // when
      paymentCallbackFacade.handleSuccess(TRANSACTION_KEY);

      // then
      Product unchangedProduct = productRepository.findById(product.getId()).orElseThrow();
      assertThat(unchangedProduct.getStockValue()).isEqualTo(originalStock);
    }

    @Test
    @DisplayName("존재하지 않는 transactionKey면 NOT_FOUND 예외가 발생한다")
    void shouldThrowNotFound_whenPaymentNotExists() {
      // given
      String invalidTransactionKey = "INVALID_TXN";

      // when & then
      assertThatThrownBy(() -> paymentCallbackFacade.handleSuccess(invalidTransactionKey))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("결제에 연결된 주문이 존재하지 않으면 NOT_FOUND 예외가 발생한다")
    void shouldThrowNotFound_whenOrderNotExists() {
      // given
      Payment orphanPayment = Payment.of(
          999999L, user.getId(), CardType.HYUNDAI, CARD_NO, 10000L, REQUESTED_AT_2025_12_01
      );
      orphanPayment.toPending("TXN_ORPHAN");
      paymentRepository.save(orphanPayment);

      // when & then
      assertThatThrownBy(() -> paymentCallbackFacade.handleSuccess("TXN_ORPHAN"))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("handleFailed")
  class HandleFailed {

    @Test
    @DisplayName("결제 실패 시 결제 상태가 FAILED가 되고 주문이 PAYMENT_FAILED가 되며 포인트가 환불된다")
    void shouldRefundPointAndFailOrder_whenFailed() {
      // given
      String reason = "카드 한도 초과";

      // when
      paymentCallbackFacade.handleFailed(TRANSACTION_KEY, reason);

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(order.getId()).orElseThrow();
        Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment)
                .extracting("status", "failureReason")
                .containsExactly(PaymentStatus.FAILED, reason),
            () -> assertThat(updatedPayment.getPgCompletedAt()).isNotNull(),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED),
            () -> assertThat(updatedPoint.getAmountValue()).isEqualTo(120000L)
        );
      });
    }

    @Test
    @DisplayName("포인트 미사용 주문이면 포인트 환불 없이 결제만 실패 처리된다")
    void shouldNotRefundPoint_whenNoPointUsed() {
      // given
      Order noPointOrder = Order.of(user.getId(), OrderStatus.PENDING, 30000L, 0L, 30000L, ORDERED_AT_2025_12_01);
      addOrderItem(noPointOrder, product.getId(), "테스트상품", 3L, 10000L);
      Order savedNoPointOrder = orderJpaRepository.save(noPointOrder);

      Payment noPointPayment = Payment.of(
          savedNoPointOrder.getId(), user.getId(), CardType.SAMSUNG, CARD_NO, 30000L, REQUESTED_AT_2025_12_01
      );
      noPointPayment.toPending("TXN_NO_POINT");
      paymentRepository.save(noPointPayment);

      Long originalAmount = pointRepository.findByUserId(user.getId()).orElseThrow().getAmountValue();

      // when
      paymentCallbackFacade.handleFailed("TXN_NO_POINT", "결제 거부");

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(5, SECONDS).untilAsserted(() -> {
        Payment updatedPayment = paymentRepository.findByTransactionKey("TXN_NO_POINT").orElseThrow();
        Order updatedOrder = orderJpaRepository.findById(savedNoPointOrder.getId()).orElseThrow();
        Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

        assertAll(
            () -> assertThat(updatedPayment).extracting("status").isEqualTo(PaymentStatus.FAILED),
            () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED),
            () -> assertThat(unchangedPoint.getAmountValue()).isEqualTo(originalAmount)
        );
      });
    }

    @Test
    @DisplayName("이미 완료된 결제면 아무 처리도 하지 않는다")
    void shouldDoNothing_whenAlreadyCompleted() {
      // given
      payment.toFailed("이전 실패 사유", REQUESTED_AT_2025_12_01.plusMinutes(1));
      paymentRepository.save(payment);

      Long originalAmount = pointRepository.findByUserId(user.getId()).orElseThrow().getAmountValue();

      // when
      paymentCallbackFacade.handleFailed(TRANSACTION_KEY, "새 실패 사유");

      // then
      Payment unchangedPayment = paymentRepository.findByTransactionKey(TRANSACTION_KEY).orElseThrow();
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

      assertAll(
          () -> assertThat(unchangedPayment.getFailureReason()).isEqualTo("이전 실패 사유"),
          () -> assertThat(unchangedPoint.getAmountValue()).isEqualTo(originalAmount)
      );
    }
  }
}
