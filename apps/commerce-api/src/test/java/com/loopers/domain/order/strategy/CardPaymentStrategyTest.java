package com.loopers.domain.order.strategy;

import com.loopers.domain.order.PaymentDomainService;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.dto.PgPaymentRequest;
import com.loopers.infrastructure.pg.dto.PgPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPaymentStrategyTest {

    @Mock
    private PgClient pgClient;

    @Mock
    private PaymentDomainService paymentDomainService;

    private CardPaymentStrategy cardPaymentStrategy;

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 100L;
    private static final String USER_ID = "user123";
    private static final Long CARD_AMOUNT = 10_000L;
    private static final String CARD_TYPE = "SAMSUNG";
    private static final String CARD_NO = "1234-5678-9814-1451";
    private static final String TRANSACTION_KEY = "20250816:TR:9577c5";

    @BeforeEach
    void setUp() {
        cardPaymentStrategy = new CardPaymentStrategy(pgClient, paymentDomainService);
        ReflectionTestUtils.setField(cardPaymentStrategy, "callbackBaseUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("카드 결제 요청 성공 시 PG 거래 ID 저장")
    void test1() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        PgPaymentResponse response = createSuccessResponse();

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenReturn(response);

        cardPaymentStrategy.executePayment(context);

        verify(paymentDomainService).updatePgTransactionId(PAYMENT_ID, TRANSACTION_KEY);
    }

    @Test
    @DisplayName("PG 요청 실패 시 예외가 발생하여 Retry/CircuitBreaker가 처리할 수 있음")
    void test2() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenThrow(new RuntimeException("PG connection failed"));

        assertThatThrownBy(() -> cardPaymentStrategy.executePayment(context))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PG connection failed");
    }

    @Test
    @DisplayName("PG 응답이 느려도 정상 처리됨 (타임아웃은 Feign 레벨에서 처리)")
    void test3() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        PgPaymentResponse response = createSuccessResponse();
        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenReturn(response);

        assertThatNoException().isThrownBy(() ->
            cardPaymentStrategy.executePayment(context)
        );

        verify(paymentDomainService).updatePgTransactionId(PAYMENT_ID, TRANSACTION_KEY);
    }

    @Test
    @DisplayName("연속 PG 실패 시 예외가 발생하여 CircuitBreaker가 처리 가능")
    void test4() {
        PaymentContext context = PaymentContext.forCardOnly(
            ORDER_ID, PAYMENT_ID, USER_ID, CARD_AMOUNT, CARD_TYPE, CARD_NO
        );

        when(pgClient.requestPayment(eq(USER_ID), any(PgPaymentRequest.class)))
            .thenThrow(new RuntimeException("PG failure"));

        assertThatThrownBy(() -> cardPaymentStrategy.executePayment(context))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("PG failure");
    }

    private PgPaymentResponse createSuccessResponse() {
        PgPaymentResponse response = new PgPaymentResponse();
        PgPaymentResponse.PgPaymentData data = new PgPaymentResponse.PgPaymentData();
        ReflectionTestUtils.setField(data, "transactionKey", TRANSACTION_KEY);
        ReflectionTestUtils.setField(data, "status", "PENDING");
        ReflectionTestUtils.setField(response, "data", data);
        return response;
    }
}
