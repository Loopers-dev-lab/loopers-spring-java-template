package com.loopers.infrastructure.payment;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.domain.payment.PaymentType;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FeignPaymentGateway E2E 테스트
 * 실제 PG simulator와 통신하며 Circuit Breaker, Fallback 동작을 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "external.pg-simulator.url=http://localhost:8082"
})
class FeignPaymentGatewayE2ETest {

    private static final String TEST_USER_ID = "user123";
    private static final String TEST_CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";

    @Autowired
    private FeignPaymentGateway paymentGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    private User testUser;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Circuit Breaker 초기화
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        circuitBreaker.reset();

        // 테스트 데이터 생성
        testUser = createTestUser();
        testOrder = createTestOrder();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("정상 결제 요청 시 Payment Result를 반환한다")
    @Test
    void shouldProcessPaymentSuccessfully() {
        // given
        Payment payment = createTestPayment();

        // when
        PaymentResult result = paymentGateway.processPayment(
                TEST_USER_ID,
                payment,
                TEST_CALLBACK_URL
        );

        // then
        assertThat(result).isNotNull();
        assertThat(result.transactionId()).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.message()).contains("결제 요청이 접수되었습니다");
    }

    @DisplayName("Circuit Breaker가 OPEN 상태일 때 fallback이 호출되어 FAIL 상태를 반환한다")
    @Test
    void shouldCallFallbackWhenCircuitIsOpen() {
        // given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        circuitBreaker.transitionToOpenState(); // Circuit을 강제로 OPEN 상태로 전환

        Payment payment = createTestPayment();

        // when
        PaymentResult result = paymentGateway.processPayment(TEST_USER_ID, payment, TEST_CALLBACK_URL);

        // then
        // Fallback이 호출되어 FAIL 상태 반환
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.message()).contains("결제 시스템 장애");
        assertThat(result.transactionId()).isNull();

        // Circuit이 OPEN 상태인지 확인
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @DisplayName("PG 장애 시 연속 실패하면 fallback이 호출되어 FAIL 상태를 반환한다")
    @Test
    void shouldCallFallbackOnConsecutivePgFailures() {
        // given
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        Payment payment = createTestPayment();

        // Circuit Breaker 초기 상태 확인
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // when: PG simulator가 다운되어 연속 실패
        // PG simulator가 없으므로 모든 호출이 실패하고 fallback이 호출됨
        int failureCount = 0;
        for (int i = 0; i < 5; i++) {
            PaymentResult result = paymentGateway.processPayment(TEST_USER_ID, payment, TEST_CALLBACK_URL);

            // fallback이 호출되어 FAIL 상태 반환
            if ("FAIL".equals(result.status())) {
                failureCount++;
                assertThat(result.message()).contains("결제 시스템 장애");
                assertThat(result.transactionId()).isNull();
            }
        }

        // then

        // 추가 호출도 fallback이 정상적으로 작동하는지 확인
        PaymentResult result = paymentGateway.processPayment(TEST_USER_ID, payment, TEST_CALLBACK_URL);
        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.message()).contains("결제 시스템 장애");
    }

    @DisplayName("Feign Client 타임아웃 설정이 적용되어 2초 내에 연결되지 않으면 fallback이 호출된다")
    @Test
    void shouldTimeoutWhenConnectionFails() {
        // given
        Payment payment = createTestPayment();
        long startTime = System.currentTimeMillis();

        // when
        // PG simulator가 없으므로 Connection Timeout 발생 → fallback 호출
        PaymentResult result = paymentGateway.processPayment(TEST_USER_ID, payment, TEST_CALLBACK_URL);

        // then
        // fallback이 호출되어 FAIL 상태 반환
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("FAIL");
        assertThat(result.message()).contains("결제 시스템 장애");

        // 타임아웃 설정이 적용되었는지 확인
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(elapsedTime)
                .isLessThan(3000); // connect timeout + read timeout 고려
    }

    /**
     * 테스트용 User 생성
     */
    private User createTestUser() {
        User user = User.createUser(
                TEST_USER_ID,
                "test@example.com",
                "1990-01-01",
                Gender.MALE
        );
        return userJpaRepository.save(user);
    }

    /**
     * 테스트용 Order 생성
     */
    private Order createTestOrder() {
        Brand brand = Brand.createBrand("Test Brand");
        Brand savedBrand = brandJpaRepository.save(brand);

        Product product = Product.createProduct(
                "TEST-001",
                "Test Product",
                Money.of(BigDecimal.valueOf(10000)),
                100,
                savedBrand
        );
        Product savedProduct = productJpaRepository.save(product);

        Map<Product, Integer> productQuantities = new HashMap<>();
        productQuantities.put(savedProduct, 1);

        Order order = Order.createOrder(testUser, productQuantities);
        order.updateStatus(OrderStatus.INIT);
        return orderJpaRepository.save(order);
    }

    /**
     * 테스트용 Payment 생성
     */
    private Payment createTestPayment() {
        return Payment.createPaymentForCard(
                testOrder,
                Money.of(BigDecimal.valueOf(10000)),
                PaymentType.CARD,
                CardType.SAMSUNG,
                "1234-5678-9012-3456"
        );
    }
}
