package com.loopers.domain.pg;

import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PgServiceTest {

    @Mock
    private PgClient pgClient;

    @InjectMocks
    private PgService pgService;

    @Nested
    @DisplayName("PG 결제 요청")
    class RequestPayment {

        @DisplayName("PG 결제 요청이 성공하면 거래 ID를 반환한다.")
        @Test
        void requestPayment_success() {
            // given
            String userId = "user123";
            String orderId = "order456";
            String cardType = "SAMSUNG";
            String cardNo = "1234-5678-9012-3456";
            String amount = "10000";
            String callbackUrl = "http://callback.url";

            PgPaymentResponse mockResponse = new PgPaymentResponse(
                    "20250105:TR:abc123",
                    "PENDING",
                    "결제 요청 완료"
            );

            given(pgClient.requestPayment(eq(userId), any(PgPaymentRequest.class)))
                    .willReturn(mockResponse);

            // when
            String transactionId = pgService.requestPayment(
                    userId, orderId, cardType, cardNo, amount, callbackUrl
            );

            // then
            assertThat(transactionId).isEqualTo("20250105:TR:abc123");
            then(pgClient).should(times(1)).requestPayment(eq(userId), any(PgPaymentRequest.class));
        }

        @DisplayName("PG 클라이언트가 null을 반환하면 예외가 발생한다.")
        @Test
        void requestPayment_nullResponse() {
            // given
            given(pgClient.requestPayment(anyString(), any(PgPaymentRequest.class)))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> pgService.requestPayment(
                    "user123", "order456", "SAMSUNG", "1234-5678", "10000", "http://callback.url"
            ))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.INTERNAL_ERROR);
        }
    }

    @Nested
    @DisplayName("PG 결제 상세 조회")
    class GetPaymentDetail {

        @DisplayName("거래 ID로 결제 상세 정보를 조회할 수 있다.")
        @Test
        void getPaymentDetail_success() {
            // given
            String userId = "user123";
            String transactionId = "20250105:TR:abc123";

            PgPaymentResponse mockResponse = new PgPaymentResponse(
                    transactionId,
                    "SUCCESS",
                    "결제 완료"
            );

            given(pgClient.getPaymentDetail(userId, transactionId))
                    .willReturn(mockResponse);

            // when
            PgPaymentResponse response = pgService.getPaymentDetail(userId, transactionId);

            // then
            assertThat(response.transactionId()).isEqualTo(transactionId);
            assertThat(response.status()).isEqualTo("SUCCESS");
        }

        @DisplayName("존재하지 않는 거래 ID로 조회하면 null을 반환한다.")
        @Test
        void getPaymentDetail_notFound() {
            // given
            given(pgClient.getPaymentDetail(anyString(), anyString()))
                    .willReturn(null);

            // when
            PgPaymentResponse response = pgService.getPaymentDetail("user123", "invalid-tx");

            // then
            assertThat(response).isNull();
        }
    }
}
