package com.loopers.infrastructure.scheduler;

import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.payment.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatusCheckServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentStatusCheckService paymentStatusCheckService;

    private Payment processingPayment;
    private String testPaymentId;

    @BeforeEach
    void setUp() {
        // PROCESSING 상태의 결제 생성
        processingPayment = Payment.createPaymentForCard(
                mock(Order.class),
                Money.of(10000L),
                PaymentType.CARD,
                CardType.SAMSUNG,
                "1234567890123456"
        );
        processingPayment.startProcessing("test-pg-transaction-id");
        testPaymentId = processingPayment.getPaymentId();
    }

    @Test
    @DisplayName("결제를 찾을 수 없으면 예외를 발생시킨다")
    void checkPaymentStatus_PaymentNotFound() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.empty());

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        // 예외 발생 시 예외 처리 블록에서도 재조회 시도 (총 2번 호출)
        verify(paymentRepository, times(2)).findByPaymentId(testPaymentId);
        verify(paymentGateway, never()).checkPaymentStatus(anyString());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("PG 응답이 SUCCESS면 결제를 완료 처리한다")
    void checkPaymentStatus_UpdateToSuccess() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment));
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willReturn(new PaymentResult("test-pg-transaction-id", "SUCCESS", "결제 완료"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(1);
        verify(paymentRepository).findByPaymentId(testPaymentId);
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        verify(paymentRepository).save(processingPayment);
    }

    @Test
    @DisplayName("PG 응답이 FAILED면 결제를 실패 처리한다")
    void checkPaymentStatus_UpdateToFailed() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment));
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willReturn(new PaymentResult("test-pg-transaction-id", "FAILED", "결제 실패"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(1);
        verify(paymentRepository).findByPaymentId(testPaymentId);
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        verify(paymentRepository).save(processingPayment);
    }

    @Test
    @DisplayName("PG 응답이 FAIL이면 결제를 실패 처리한다")
    void checkPaymentStatus_UpdateToFail() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment));
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willReturn(new PaymentResult("test-pg-transaction-id", "FAIL", "카드 승인 거부"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(1);
        verify(paymentRepository).findByPaymentId(testPaymentId);
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        verify(paymentRepository).save(processingPayment);
    }

    @Test
    @DisplayName("PG 응답이 PROCESSING이면 상태를 유지하고 확인 횟수만 증가한다")
    void checkPaymentStatus_StillProcessing() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment));
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willReturn(new PaymentResult("test-pg-transaction-id", "PROCESSING", "결제 처리 중"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(1);
        verify(paymentRepository).findByPaymentId(testPaymentId);
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        verify(paymentRepository).save(processingPayment);
    }

    @Test
    @DisplayName("PG 상태 확인 중 예외가 발생해도 확인 횟수는 증가하고 저장한다")
    void checkPaymentStatus_ExceptionDuringPGCheck() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment))
                .willReturn(Optional.of(processingPayment)); // 예외 블록에서 재조회
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willThrow(new RuntimeException("PG 시스템 장애"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatus()).isEqualTo(PaymentStatus.PROCESSING); // 상태 유지
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(1); // 확인 횟수 증가
        verify(paymentRepository, times(2)).findByPaymentId(testPaymentId); // 초기 조회 + 예외 블록 재조회
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        verify(paymentRepository, times(1)).save(processingPayment); // 에러 처리 블록에서 저장
    }

    @Test
    @DisplayName("확인 횟수는 PG 호출 후 항상 증가한다")
    void checkPaymentStatus_IncrementCheckCount() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment));
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willReturn(new PaymentResult("test-pg-transaction-id", "SUCCESS", "결제 완료"));

        int initialCheckCount = processingPayment.getStatusCheckCount();

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        assertThat(processingPayment.getStatusCheckCount()).isEqualTo(initialCheckCount + 1);
        verify(paymentRepository).save(processingPayment);
    }

    @Test
    @DisplayName("예외 발생 시 확인 횟수 증가 중 또 예외가 발생하면 로그만 남긴다")
    void checkPaymentStatus_ExceptionDuringIncrementCheckCount() {
        // given
        given(paymentRepository.findByPaymentId(testPaymentId))
                .willReturn(Optional.of(processingPayment))
                .willThrow(new RuntimeException("DB 연결 실패")); // 재조회 시 예외
        given(paymentGateway.checkPaymentStatus("test-pg-transaction-id"))
                .willThrow(new RuntimeException("PG 시스템 장애"));

        // when
        paymentStatusCheckService.checkPaymentStatus(testPaymentId);

        // then
        verify(paymentRepository, times(2)).findByPaymentId(testPaymentId);
        verify(paymentGateway).checkPaymentStatus("test-pg-transaction-id");
        // 예외 처리 블록 내부에서도 예외 발생하면 저장 안 됨 (로그만 남김)
    }
}
