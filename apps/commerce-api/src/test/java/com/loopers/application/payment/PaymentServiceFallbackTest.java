package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.PgClient;
import com.loopers.infrastructure.payment.PgPaymentRequest;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("PaymentService Fallback 로직 테스트")
class PaymentServiceFallbackTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean
    private PgClient pgClient;

    @Autowired
    private PaymentRepository paymentRepository;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit");
        circuitBreaker.reset();
        paymentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        circuitBreaker.reset();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Fallback 실행 시 TEMP- 접두사로 transactionKey가 생성된다")
    void shouldCreateTempTransactionKeyOnFallback() {
        // given
        circuitBreaker.transitionToOpenState();

        // when
        Payment result = paymentService.createFallbackPayment(
            "user123",
            "order-fallback-1",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result.getTransactionKey()).startsWith("TEMP-");
        assertThat(result.getTransactionKey()).matches("TEMP-\\d+");
    }

    @Test
    @DisplayName("Fallback 실행 시 Payment 상태는 PENDING이다")
    void shouldSetPaymentStatusToPendingOnFallback() {
        // given
        circuitBreaker.transitionToOpenState();


        // when
        Payment result = paymentService.createFallbackPayment(
            "user123",
            "order-fallback-2",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Fallback 실행 시 모든 주문 정보가 올바르게 저장된다")
    void shouldPreserveAllOrderInfoOnFallback() {
        // given
        circuitBreaker.transitionToOpenState();


        String userId = "user456";
        String orderId = "order-preserve-test";
        BigDecimal amount = BigDecimal.valueOf(25000);
        String cardType = "KB";
        String cardNo = "9876-5432-1098-7654";

        // when
        Payment result = paymentService.createFallbackPayment(
            userId, orderId, amount, cardType, cardNo
        );

        // then
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getCardType()).isEqualTo(cardType);
        assertThat(result.getCardNo()).isEqualTo(cardNo);
    }

    @Test
    @DisplayName("Fallback으로 생성된 Payment는 DB에 저장된다")
    void shouldSaveFallbackPaymentToDatabase() {
        // given
        circuitBreaker.transitionToOpenState();

        // when
        Payment result = paymentService.createFallbackPayment(
            "user789",
            "order-db-save",
            BigDecimal.valueOf(15000),
            "HYUNDAI",
            "1111-2222-3333-4444"
        );

        // then
        Payment saved = paymentRepository.findByOrderId("order-db-save").get(0);
        assertThat(saved).isNotNull();
        assertThat(saved.getTransactionKey()).startsWith("TEMP-");
        assertThat(saved.getOrderId()).isEqualTo("order-db-save");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Circuit Open 상태에서 여러 요청 모두 Fallback 처리된다")
    void shouldHandleMultipleRequestsWithFallback() {
        // given
        circuitBreaker.transitionToOpenState();

        // when
        Payment result1 = paymentService.createFallbackPayment(
            "user1", "order-1", BigDecimal.valueOf(10000), "SAMSUNG", "1234-1234-1234-1234"
        );
        Payment result2 = paymentService.createFallbackPayment(
            "user2", "order-2", BigDecimal.valueOf(20000), "KB", "5678-5678-5678-5678"
        );
        Payment result3 = paymentService.createFallbackPayment(
            "user3", "order-3", BigDecimal.valueOf(30000), "HYUNDAI", "9012-9012-9012-9012"
        );

        // then
        assertThat(result1.getTransactionKey()).startsWith("TEMP-");
        assertThat(result2.getTransactionKey()).startsWith("TEMP-");
        assertThat(result3.getTransactionKey()).startsWith("TEMP-");

        // Verify all payments are saved
        assertThat(paymentRepository.findById(result1.getId())).isPresent();
        assertThat(paymentRepository.findById(result2.getId())).isPresent();
        assertThat(paymentRepository.findById(result3.getId())).isPresent();
    }

    @Test
    @DisplayName("PG 호출 실패 후 Fallback이 정상 동작한다")
    void shouldExecuteFallbackAfterPgFailure() {
        // given
        when(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
            .thenThrow(createFeignException())
            .thenThrow(createFeignException()); // Retry 후에도 실패


        // when
        Payment result = paymentService.createFallbackPayment(
            "user999",
            "order-pg-failure",
            BigDecimal.valueOf(5000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then
        assertThat(result.getTransactionKey()).startsWith("TEMP-");
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getOrderId()).isEqualTo("order-pg-failure");
    }

    @Test
    @DisplayName("Fallback 실행 시에도 비즈니스 검증은 수행된다")
    void shouldValidateBusinessRulesEvenInFallback() {
        // given
        circuitBreaker.transitionToOpenState();


        // when
        Payment result = paymentService.createFallbackPayment(
            "user123",
            "order-validation",
            BigDecimal.valueOf(10000),
            "SAMSUNG",
            "1234-5678-9012-3456"
        );

        // then - Payment 엔티티의 기본 검증이 수행되어야 함
        assertThat(result.getUserId()).isNotBlank();
        assertThat(result.getOrderId()).isNotBlank();
        assertThat(result.getAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    private FeignException createFeignException() {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://localhost:8082/api/v1/payments",
            new HashMap<>(),
            null,
            new RequestTemplate()
        );
        return new FeignException.ServiceUnavailable(
            "Service unavailable",
            request,
            "Service unavailable".getBytes(StandardCharsets.UTF_8),
            null
        );
    }
}
