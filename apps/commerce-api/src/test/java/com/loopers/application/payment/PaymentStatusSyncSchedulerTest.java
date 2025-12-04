package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.PgClient;
import com.loopers.infrastructure.payment.PgTransactionDetail;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("PaymentStatusSyncScheduler 테스트")
class PaymentStatusSyncSchedulerTest {

    @Autowired
    private PaymentStatusSyncScheduler scheduler;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PgClient pgClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("PENDING 상태의 결제를 조회하여 동기화한다")
    void shouldSyncPendingPayments() {
        // given
        Payment payment1 = createPayment("user1", "order1", "TX-111", PaymentStatus.PENDING);
        Payment payment2 = createPayment("user2", "order2", "TX-222", PaymentStatus.PENDING);
        paymentRepository.save(payment1);
        paymentRepository.save(payment2);

        when(pgClient.getPaymentDetail(anyString(), anyString()))
            .thenReturn(new PgTransactionDetail(
                "TX-111", "order1", "SAMSUNG", "1234-5678-9012-3456", 10000L, "SUCCESS", null
            ))
            .thenReturn(new PgTransactionDetail(
                "TX-222", "order2", "KB", "1234-5678-9012-3456", 20000L, "FAILED", "한도 초과"
            ));

        // when
        scheduler.syncPendingPayments();

        // then
        Payment updated1 = paymentRepository.findByTransactionKey("TX-111").orElseThrow();
        Payment updated2 = paymentRepository.findByTransactionKey("TX-222").orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updated2.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated2.getFailureReason()).isEqualTo("한도 초과");

        verify(pgClient, times(2)).getPaymentDetail(anyString(), anyString());
    }

    @Test
    @DisplayName("TEMP- 트랜잭션은 건너뛴다")
    void shouldSkipTempTransactions() {
        // given
        Payment tempPayment = createPayment("user1", "order1", "TEMP-123456", PaymentStatus.PENDING);
        Payment normalPayment = createPayment("user2", "order2", "TX-111", PaymentStatus.PENDING);
        paymentRepository.save(tempPayment);
        paymentRepository.save(normalPayment);

        when(pgClient.getPaymentDetail(anyString(), anyString()))
            .thenReturn(new PgTransactionDetail(
                "TX-111", "order2", "SAMSUNG", "1234-5678-9012-3456", 10000L, "SUCCESS", null
            ));

        // when
        scheduler.syncPendingPayments();

        // then
        Payment tempResult = paymentRepository.findByTransactionKey("TEMP-123456").orElseThrow();
        assertThat(tempResult.getStatus()).isEqualTo(PaymentStatus.PENDING); // 변경 없음

        // TEMP-에 대해서는 PG 호출 안함, 정상 트랜잭션에 대해서만 1번 호출
        verify(pgClient, times(1)).getPaymentDetail(anyString(), anyString());
        verify(pgClient, never()).getPaymentDetail(anyString(), eq("TEMP-123456"));
    }

    @Test
    @DisplayName("SUCCESS 또는 FAILED 상태는 동기화하지 않는다")
    void shouldNotSyncCompletedPayments() {
        // given
        Payment successPayment = createPayment("user1", "order1", "TX-111", PaymentStatus.SUCCESS);
        Payment failedPayment = createPayment("user2", "order2", "TX-222", PaymentStatus.FAILED);
        paymentRepository.save(successPayment);
        paymentRepository.save(failedPayment);

        // when
        scheduler.syncPendingPayments();

        // then
        verify(pgClient, never()).getPaymentDetail(anyString(), anyString());
    }

    @Test
    @DisplayName("PENDING 결제가 없으면 PG를 호출하지 않는다")
    void shouldNotCallPgWhenNoPendingPayments() {
        // given - PENDING 결제 없음

        // when
        scheduler.syncPendingPayments();

        // then
        verify(pgClient, never()).getPaymentDetail(anyString(), anyString());
    }

    @Test
    @DisplayName("일부 결제 동기화 실패해도 다른 결제는 정상 처리한다")
    void shouldContinueSyncingEvenIfSomeFail() {
        // given
        Payment payment1 = createPayment("user1", "order1", "TX-111", PaymentStatus.PENDING);
        Payment payment2 = createPayment("user2", "order2", "TX-222", PaymentStatus.PENDING);
        Payment payment3 = createPayment("user3", "order3", "TX-333", PaymentStatus.PENDING);
        paymentRepository.save(payment1);
        paymentRepository.save(payment2);
        paymentRepository.save(payment3);

        when(pgClient.getPaymentDetail(anyString(), anyString()))
            .thenReturn(new PgTransactionDetail(
                "TX-111", "order1", "SAMSUNG", "1234-5678-9012-3456", 10000L, "SUCCESS", null
            ))
            .thenThrow(new RuntimeException("PG 호출 실패"))
            .thenReturn(new PgTransactionDetail(
                "TX-333", "order3", "HYUNDAI", "1234-5678-9012-3456", 30000L, "SUCCESS", null
            ));

        // when
        scheduler.syncPendingPayments();

        // then
        Payment updated1 = paymentRepository.findByTransactionKey("TX-111").orElseThrow();
        Payment updated2 = paymentRepository.findByTransactionKey("TX-222").orElseThrow();
        Payment updated3 = paymentRepository.findByTransactionKey("TX-333").orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updated2.getStatus()).isEqualTo(PaymentStatus.PENDING); // 실패로 변경 안됨
        assertThat(updated3.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        verify(pgClient, times(3)).getPaymentDetail(anyString(), anyString());
    }

    @Test
    @DisplayName("이미 완료된 상태로 동기화된 결제는 다시 조회하지 않는다")
    void shouldNotQueryAlreadySyncedPayments() {
        // given
        Payment payment = createPayment("user1", "order1", "TX-111", PaymentStatus.PENDING);
        paymentRepository.save(payment);

        when(pgClient.getPaymentDetail(anyString(), anyString()))
            .thenReturn(new PgTransactionDetail(
                "TX-111", "order1", "SAMSUNG", "1234-5678-9012-3456", 10000L, "SUCCESS", null
            ));

        // when - 첫 번째 실행
        scheduler.syncPendingPayments();

        // then
        verify(pgClient, times(1)).getPaymentDetail(anyString(), anyString());

        // when - 두 번째 실행
        scheduler.syncPendingPayments();

        // then - 추가 호출 없음 (여전히 1번)
        verify(pgClient, times(1)).getPaymentDetail(anyString(), anyString());

        Payment updated = paymentRepository.findByTransactionKey("TX-111").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("PG에서 여전히 PENDING 상태면 로컬도 PENDING으로 유지")
    void shouldKeepPendingStatusIfPgStillPending() {
        // given
        Payment payment = createPayment("user1", "order1", "TX-111", PaymentStatus.PENDING);
        paymentRepository.save(payment);

        when(pgClient.getPaymentDetail(anyString(), anyString()))
            .thenReturn(new PgTransactionDetail(
                "TX-111", "order1", "SAMSUNG", "1234-5678-9012-3456", 10000L, "PENDING", null
            ));

        // when
        scheduler.syncPendingPayments();

        // then
        Payment updated = paymentRepository.findByTransactionKey("TX-111").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    private Payment createPayment(String userId, String orderId, String transactionKey, PaymentStatus status) {
        return paymentRepository.save(Payment.builder()
            .userId(userId)
            .orderId(orderId)
            .transactionKey(transactionKey)
            .amount(BigDecimal.valueOf(10000))
            .status(status)
            .cardType("SAMSUNG")
            .cardNo("1234-5678-9012-3456")
            .build());
    }
}
