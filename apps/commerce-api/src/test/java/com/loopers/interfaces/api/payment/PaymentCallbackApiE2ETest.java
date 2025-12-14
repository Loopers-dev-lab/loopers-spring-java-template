package com.loopers.interfaces.api.payment;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("PaymentCallback API E2E 테스트")
class PaymentCallbackApiE2ETest {

  private static final String BASE_URL = "/api/v1/payments/callback";
  private static final int ASYNC_EVENT_TIMEOUT_SECONDS = 10;
  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  private static final LocalDateTime ORDERED_AT_2025_12_01 = LocalDateTime.of(2025, 12, 1, 10, 0, 0);
  private static final LocalDateTime REQUESTED_AT_2025_12_01 = LocalDateTime.of(2025, 12, 1, 10, 5, 0);
  private static final String CARD_NO = "1234-5678-9012-3456";
  private static final ParameterizedTypeReference<ApiResponse<Void>> VOID_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final TestRestTemplate testRestTemplate;
  private final PaymentRepository paymentRepository;
  private final OrderJpaRepository orderJpaRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final PointRepository pointRepository;
  private final BrandJpaRepository brandJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  PaymentCallbackApiE2ETest(
      TestRestTemplate testRestTemplate,
      PaymentRepository paymentRepository,
      OrderJpaRepository orderJpaRepository,
      ProductRepository productRepository,
      UserRepository userRepository,
      PointRepository pointRepository,
      BrandJpaRepository brandJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.paymentRepository = paymentRepository;
    this.orderJpaRepository = orderJpaRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.pointRepository = pointRepository;
    this.brandJpaRepository = brandJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("POST /api/v1/payments/callback")
  class PostCallback {

    @Test
    @DisplayName("SUCCESS 콜백이면 200 OK를 반환하고 결제가 성공 처리된다")
    void shouldReturn200AndCompletePayment_whenSuccessCallback() {
      // given
      String transactionKey = "TXN_E2E_SUCCESS_" + UUID.randomUUID();

      User user = saveUser("testuser", "test@example.com");
      Brand brand = saveBrand("테스트브랜드");
      Product product = saveProduct("테스트상품", 10000L, 10L, brand.getId());
      savePoint(user.getId(), 100000L);

      Order order = Order.of(user.getId(), OrderStatus.PENDING, 30000L, 20000L, 10000L, ORDERED_AT_2025_12_01);
      addOrderItem(order, product.getId(), "테스트상품", 3L, 10000L);
      Order savedOrder = orderJpaRepository.save(order);
      Long orderId = savedOrder.getId();

      Payment payment = Payment.of(orderId, user.getId(), CardType.SAMSUNG, CARD_NO, 10000L, REQUESTED_AT_2025_12_01);
      payment.toPending(transactionKey);
      paymentRepository.save(payment);

      PaymentCallbackRequest request = new PaymentCallbackRequest(
          transactionKey, orderId.toString(), "SAMSUNG", CARD_NO, 10000L, "SUCCESS", null
      );

      // when
      ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
          BASE_URL,
          HttpMethod.POST,
          new HttpEntity<>(request),
          VOID_RESPONSE_TYPE
      );

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      Payment updatedPayment = paymentRepository.findByTransactionKey(transactionKey).orElseThrow();
      assertThat(updatedPayment).extracting("status").isEqualTo(PaymentStatus.SUCCESS);

      // 비동기 이벤트 핸들러 완료 대기 (주문 완료, 재고 차감)
      await()
          .atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS)
          .pollInterval(100, MILLISECONDS)
          .untilAsserted(() -> {
            Order updatedOrder = orderJpaRepository.findById(orderId).orElseThrow();
            Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();

            assertAll(
                () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.COMPLETED),
                () -> assertThat(updatedProduct.getStockValue()).isEqualTo(7L)
            );
          });
    }

    @Test
    @DisplayName("FAILED 콜백이면 200 OK를 반환하고 결제가 실패 처리된다")
    void shouldReturn200AndFailPayment_whenFailedCallback() {
      // given
      String transactionKey = "TXN_E2E_FAILED_" + UUID.randomUUID();

      User user = saveUser("testuser", "test@example.com");
      Brand brand = saveBrand("테스트브랜드");
      Product product = saveProduct("테스트상품", 10000L, 10L, brand.getId());
      savePoint(user.getId(), 100000L);

      Order order = Order.of(user.getId(), OrderStatus.PENDING, 30000L, 20000L, 10000L, ORDERED_AT_2025_12_01);
      addOrderItem(order, product.getId(), "테스트상품", 3L, 10000L);
      Order savedOrder = orderJpaRepository.save(order);
      Long orderId = savedOrder.getId();
      Long userId = user.getId();

      Payment payment = Payment.of(orderId, userId, CardType.SAMSUNG, CARD_NO, 10000L, REQUESTED_AT_2025_12_01);
      payment.toPending(transactionKey);
      paymentRepository.save(payment);

      PaymentCallbackRequest request = new PaymentCallbackRequest(
          transactionKey, orderId.toString(), "SAMSUNG", CARD_NO, 10000L, "FAILED", "카드 한도 초과"
      );

      // when
      ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
          BASE_URL,
          HttpMethod.POST,
          new HttpEntity<>(request),
          VOID_RESPONSE_TYPE
      );

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

      Payment updatedPayment = paymentRepository.findByTransactionKey(transactionKey).orElseThrow();
      assertThat(updatedPayment)
          .extracting("status", "failureReason")
          .containsExactly(PaymentStatus.FAILED, "카드 한도 초과");

      // 비동기 이벤트 핸들러 완료 대기 (주문 실패, 포인트 환불)
      await()
          .atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS)
          .pollInterval(100, MILLISECONDS)
          .untilAsserted(() -> {
            Order updatedOrder = orderJpaRepository.findById(orderId).orElseThrow();
            Point updatedPoint = pointRepository.findByUserId(userId).orElseThrow();

            assertAll(
                () -> assertThat(updatedOrder).extracting("status").isEqualTo(OrderStatus.PAYMENT_FAILED),
                () -> assertThat(updatedPoint.getAmountValue()).isEqualTo(120000L)
            );
          });
    }

    @Test
    @DisplayName("존재하지 않는 transactionKey면 404 NOT_FOUND를 반환한다")
    void shouldReturn404_whenPaymentNotFound() {
      // given
      PaymentCallbackRequest request = new PaymentCallbackRequest(
          "INVALID_TXN", "12345", "SAMSUNG", CARD_NO, 10000L, "SUCCESS", null
      );

      // when
      ResponseEntity<ApiResponse<Void>> response = testRestTemplate.exchange(
          BASE_URL,
          HttpMethod.POST,
          new HttpEntity<>(request),
          VOID_RESPONSE_TYPE
      );

      // then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  private User saveUser(String loginId, String email) {
    User user = User.of(loginId, email, BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 12, 1));
    return userRepository.save(user);
  }

  private Brand saveBrand(String name) {
    Brand brand = Brand.of(name, name + " 설명");
    return brandJpaRepository.save(brand);
  }

  private Product saveProduct(String name, Long price, Long stock, Long brandId) {
    Product product = Product.of(name, Money.of(price), name + " 설명", Stock.of(stock), brandId);
    return productRepository.save(product);
  }

  private Point savePoint(Long userId, Long amount) {
    Point point = Point.of(userId, amount);
    return pointRepository.save(point);
  }

  private void addOrderItem(Order order, Long productId, String productName, Long quantity, Long price) {
    OrderItem item = OrderItem.of(productId, productName, Quantity.of(quantity), OrderPrice.of(price));
    order.addItem(item);
  }
}
