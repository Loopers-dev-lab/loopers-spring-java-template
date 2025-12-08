package com.loopers.domain.payment;

import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @DisplayName("결제 정보 저장 시 결제 방법은 필수값이다")
    @Test
    void createPaymentWithInvalidPaymentType_throwException() {
        // given
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(1L);

        // when & then
        CoreException result = assertThrows(CoreException.class, () -> {
            Payment payment = Payment.builder()
                    .paymentId("1234-123-4242")
                    .amount(Money.of(30000))
                    .order(order)
                    .paymentType(null)
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("결제 방식은 필수값입니다");
    }

    @DisplayName("결제 방법이 카드인 경우 카드사 정보는 필수값이다")
    @Test
    void createPaymentWithInvalidCardType_throwException() {
        // given
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(1L);

        // when & then
        CoreException result = assertThrows(CoreException.class, () -> {
            Payment payment = Payment.builder()
                    .paymentId("1234-123-4242")
                    .amount(Money.of(30000))
                    .order(order)
                    .paymentType(PaymentType.CARD)
                    .cardType(null)
                    .cardNo("1234-1234-1234-1234")
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("카드사 정보는 필수값입니다");
    }

    @DisplayName("결제 방법이 카드인 경우 카드 번호는 필수값이다")
    @Test
    void createPaymentWithInvalidCardNo_throwException() {
        // given
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(1L);

        // when & then
        CoreException result = assertThrows(CoreException.class, () -> {
            Payment payment = Payment.builder()
                    .paymentId("1234-123-4242")
                    .amount(Money.of(30000))
                    .order(order)
                    .paymentType(PaymentType.CARD)
                    .cardType(CardType.HYUNDAI)
                    .cardNo(null)
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("카드 번호는 필수값입니다");
    }

    @DisplayName("결제 방법이 카드인 경우 카드 번호가 빈 문자열이면 예외가 발생한다")
    @Test
    void createPaymentWithBlankCardNo_throwException() {
        // given
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(1L);

        // when & then
        CoreException result = assertThrows(CoreException.class, () -> {
            Payment payment = Payment.builder()
                    .paymentId("1234-123-4242")
                    .amount(Money.of(30000))
                    .order(order)
                    .paymentType(PaymentType.CARD)
                    .cardType(CardType.HYUNDAI)
                    .cardNo("")
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("카드 번호는 필수값입니다");
    }

    @DisplayName("결제가 완료되면 결재 상태값이 성공으로 바뀐다")
    @Test
    void whenPaymentComplete_paymentStatusIsSuccess(){
        // given
        Payment payment = Payment.builder()
                .paymentId("1234-123-4242")
                .amount(Money.of(30000))
                .order(Mockito.mock(Order.class))
                .paymentType(PaymentType.CARD)
                .cardType(CardType.HYUNDAI)
                .cardNo("1234-1234-1234-1234")
                .build();

        // 결제 처리 시작 (PG 요청 후)
        payment.startProcessing("test-pg-transaction-123");

        // when
        payment.completePayment();

        //then
        assertThat(payment.getCompletedAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPgTransactionId()).isEqualTo("test-pg-transaction-123");
    }

    @DisplayName("포인트 결제는 카드 정보 없이 생성 가능하다")
    @Test
    void createPaymentWithPointType_success() {
        // given
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getId()).thenReturn(1L);

        // when
        Payment payment = Payment.builder()
                .paymentId("1234-123-4242")
                .amount(Money.of(30000))
                .order(order)
                .paymentType(PaymentType.POINT)
                .build();

        // then
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT);
        assertThat(payment.getCardType()).isNull();
        assertThat(payment.getCardNo()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

}
