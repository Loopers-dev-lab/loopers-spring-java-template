package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentDomainService 테스트")
class PaymentDomainServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @InjectMocks
    PaymentDomainService paymentDomainService;

    static final Long ORDER_ID = 1L;
    static final Long PAYMENT_ID = 100L;
    static final String USER_ID = "user123";
    static final Long AMOUNT = 10_000L;

    @Nested
    @DisplayName("createPayment()")
    class CreatePaymentTest {

        @Test
        @DisplayName("결제 생성 성공")
        void test1() {
            Payment createdPayment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            when(paymentRepository.save(any(Payment.class))).thenReturn(createdPayment);

            Payment payment = paymentDomainService.createPayment(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);

            assertThat(payment).isNotNull();
            assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("markAsSuccess()")
    class MarkAsSuccessTest {

        @Test
        @DisplayName("결제 성공 처리")
        void test1() {
            Payment payment = Payment.create(PAYMENT_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            paymentDomainService.markAsSuccess(PAYMENT_ID, "PG_TXN_12345");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getPgTransactionId()).isEqualTo("PG_TXN_12345");
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("존재하지 않는 결제 성공 처리 실패")
        void test2() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentDomainService.markAsSuccess(PAYMENT_ID, "PG_TXN_12345"))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("markAsFailed()")
    class MarkAsFailedTest {

        @Test
        @DisplayName("결제 실패 처리")
        void test1() {
            Payment payment = Payment.create(PAYMENT_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            paymentDomainService.markAsFailed(PAYMENT_ID, "포인트 부족");

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo("포인트 부족");
            verify(paymentRepository).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId()")
    class GetPaymentByOrderIdTest {

        @Test
        @DisplayName("주문 ID로 결제 조회")
        void test1() {
            Payment payment = Payment.create(ORDER_ID, USER_ID, AMOUNT, PaymentType.POINT_ONLY);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            Payment result = paymentDomainService.getPaymentByOrderId(ORDER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            verify(paymentRepository).findByOrderId(ORDER_ID);
        }

        @Test
        @DisplayName("존재하지 않는 주문의 결제 조회 실패")
        void test2() {
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentDomainService.getPaymentByOrderId(ORDER_ID))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("찾을 수 없습니다");
        }
    }
}
