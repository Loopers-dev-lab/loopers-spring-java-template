package com.loopers.infra;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.OrderService;
import com.loopers.infrastructure.PgClient;
import com.loopers.infrastructure.ResilientPgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PaymentResilienceTest {

    @Autowired
    private ResilientPgClient resilientPgClient;

    @MockitoBean
    private PgClient pgClient;

    private OrderService orderService;

    private PaymentV1Dto.PgPaymentRequest request;

    @BeforeEach
    public void setUp() {
         request = new PaymentV1Dto.PgPaymentRequest(
                        1L,
                        "SAMSUNG",
                        "1234-5678-9814-1451",
                        Money.of(5000),
                        "http://localhost:8080/api/v1/payments/callback"
                );
    }

    @DisplayName("CircuitBreaker 관련 테스트")
    @Nested
    class CircuitBreaker {

        @DisplayName("Retry가 3번까지 호출된다면 성공이다.")
        @Test
        public void succeeds_whenRetryExecutesThreeTimes() {

            // arrange
            when(pgClient.requestPayment(any()))
                    .thenThrow(new RuntimeException("PG 서버 오류"));

            // act
            assertThrows(RuntimeException.class, () -> resilientPgClient.requestPayment(request));

            // assert
            verify(pgClient, times(3)).requestPayment(any());
        }
    }
}
