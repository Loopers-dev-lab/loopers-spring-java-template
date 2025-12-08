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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatusCheckSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusCheckService paymentStatusCheckService;

    @InjectMocks
    private PaymentStatusCheckScheduler scheduler;

    private Payment processingPayment;

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
    }

    @Test
    @DisplayName("PROCESSING 상태인 결제가 없으면 아무 작업도 수행하지 않는다")
    void checkProcessingPayments_NoPayments() {
        // given
        given(paymentRepository.findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt()))
                .willReturn(Collections.emptyList());

        // when
        scheduler.checkProcessingPayments();

        // then
        verify(paymentRepository).findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt());
        verify(paymentStatusCheckService, never()).checkPaymentStatus(anyString());
    }

    @Test
    @DisplayName("PROCESSING 상태인 결제가 있으면 PaymentStatusCheckService를 호출한다")
    void checkProcessingPayments_CallsService() {
        // given
        List<Payment> processingPayments = Collections.singletonList(processingPayment);

        given(paymentRepository.findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt()))
                .willReturn(processingPayments);

        // when
        scheduler.checkProcessingPayments();

        // then
        verify(paymentRepository).findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt());
        verify(paymentStatusCheckService).checkPaymentStatus(processingPayment.getPaymentId());
    }

    @Test
    @DisplayName("여러 개의 PROCESSING 결제를 순차적으로 Service에 위임한다")
    void checkProcessingPayments_MultiplePayments() {
        // given
        Payment payment1 = Payment.createPaymentForCard(
                mock(Order.class),
                Money.of(10000L),
                PaymentType.CARD,
                CardType.SAMSUNG,
                "1234567890123456"
        );
        payment1.startProcessing("pg-tx-1");

        Payment payment2 = Payment.createPaymentForCard(
                mock(Order.class),
                Money.of(20000L),
                PaymentType.CARD,
                CardType.KB,
                "9876543210987654"
        );
        payment2.startProcessing("pg-tx-2");

        List<Payment> processingPayments = Arrays.asList(payment1, payment2);

        given(paymentRepository.findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt()))
                .willReturn(processingPayments);

        // when
        scheduler.checkProcessingPayments();

        // then
        verify(paymentRepository).findProcessingPaymentsForStatusCheck(any(LocalDateTime.class), anyInt());
        verify(paymentStatusCheckService).checkPaymentStatus(payment1.getPaymentId());
        verify(paymentStatusCheckService).checkPaymentStatus(payment2.getPaymentId());
    }
}