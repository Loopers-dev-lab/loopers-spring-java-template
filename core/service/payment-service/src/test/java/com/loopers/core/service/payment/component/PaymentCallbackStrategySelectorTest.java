package com.loopers.core.service.payment.component;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 콜백 처리 전략 선택기")
class PaymentCallbackStrategySelectorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SuccessfulPaymentStrategy successfulPaymentStrategy;

    @Mock
    private FailedPaymentStrategy failedPaymentStrategy;

    @Mock
    private DuplicateSuccessPaymentStrategy duplicateSuccessPaymentStrategy;

    @InjectMocks
    private PaymentCallbackStrategySelector strategySelector;

    @Nested
    @DisplayName("select 메서드")
    class SelectMethod {

        @Nested
        @DisplayName("이미 성공한 결제가 있는 경우")
        class WhenHasSuccessfulPayment {

            @Test
            @DisplayName("DuplicateSuccessPaymentStrategy를 반환한다")
            void shouldReturnDuplicateSuccessPaymentStrategy() {
                // Given
                OrderKey orderKey = new OrderKey("order-1");
                PaymentStatus status = PaymentStatus.SUCCESS;
                Payment successfulPayment = org.mockito.Mockito.mock(Payment.class);

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.of(successfulPayment)); // 이미 성공한 결제 존재

                // When
                PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);

                // Then
                assertThat(strategy).isEqualTo(duplicateSuccessPaymentStrategy);
                verify(paymentRepository).findBy(orderKey, PaymentStatus.SUCCESS);
            }

            @Test
            @DisplayName("다른 상태값이 전달되어도 DuplicateSuccessPaymentStrategy를 반환한다")
            void shouldReturnDuplicateSuccessStrategyRegardlessOfStatus() {
                // Given
                OrderKey orderKey = new OrderKey("order-1");
                PaymentStatus status = PaymentStatus.FAILED;
                Payment successfulPayment = org.mockito.Mockito.mock(Payment.class);

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.of(successfulPayment)); // 이미 성공한 결제 존재

                // When
                PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);

                // Then
                assertThat(strategy).isEqualTo(duplicateSuccessPaymentStrategy);
            }
        }

        @Nested
        @DisplayName("성공한 결제가 없고 현재 결제가 실패한 경우")
        class WhenPaymentFailedAndNoSuccessfulPayment {

            @Test
            @DisplayName("FailedPaymentStrategy를 반환한다")
            void shouldReturnFailedPaymentStrategy() {
                // Given
                OrderKey orderKey = new OrderKey("order-2");
                PaymentStatus status = PaymentStatus.FAILED;

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.empty()); // 성공한 결제 없음

                // When
                PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);

                // Then
                assertThat(strategy).isEqualTo(failedPaymentStrategy);
                verify(paymentRepository).findBy(orderKey, PaymentStatus.SUCCESS);
            }

            @Test
            @DisplayName("PENDING 상태인 경우도 FailedPaymentStrategy를 반환한다")
            void shouldReturnFailedPaymentStrategyForPendingStatus() {
                // Given
                OrderKey orderKey = new OrderKey("order-3");
                PaymentStatus status = PaymentStatus.PENDING;

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.empty());

                // When
                PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);

                // Then
                assertThat(strategy).isEqualTo(failedPaymentStrategy);
            }
        }

        @Nested
        @DisplayName("성공한 결제가 없고 현재 결제가 성공한 경우")
        class WhenPaymentSuccessfulAndNoSuccessfulPayment {

            @Test
            @DisplayName("SuccessfulPaymentStrategy를 반환한다")
            void shouldReturnSuccessfulPaymentStrategy() {
                // Given
                OrderKey orderKey = new OrderKey("order-4");
                PaymentStatus status = PaymentStatus.SUCCESS;

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.empty()); // 성공한 결제 없음

                // When
                PaymentCallbackStrategy strategy = strategySelector.select(orderKey, status);

                // Then
                assertThat(strategy).isEqualTo(successfulPaymentStrategy);
                verify(paymentRepository).findBy(orderKey, PaymentStatus.SUCCESS);
            }
        }

        @Nested
        @DisplayName("조건 우선순위 검증")
        class ConditionPriority {

            @Test
            @DisplayName("이미 성공한 결제가 있으면 상태값과 관계없이 DuplicateSuccessPaymentStrategy를 반환한다")
            void shouldPrioritizeDuplicateSuccessOverStatus() {
                // Given
                OrderKey orderKey = new OrderKey("order-5");
                Payment successfulPayment = org.mockito.Mockito.mock(Payment.class);

                when(paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS))
                        .thenReturn(Optional.of(successfulPayment)); // 이미 성공한 결제 존재

                // When - 현재 상태가 SUCCESS인 경우
                PaymentCallbackStrategy strategyForSuccess =
                        strategySelector.select(orderKey, PaymentStatus.SUCCESS);

                // When - 현재 상태가 FAILED인 경우
                PaymentCallbackStrategy strategyForFailed =
                        strategySelector.select(orderKey, PaymentStatus.FAILED);

                // Then - 둘 다 DuplicateSuccessPaymentStrategy 반환
                assertThat(strategyForSuccess).isEqualTo(duplicateSuccessPaymentStrategy);
                assertThat(strategyForFailed).isEqualTo(duplicateSuccessPaymentStrategy);
            }

            @Test
            @DisplayName("성공한 결제가 없을 때만 상태값에 따라 전략을 선택한다")
            void shouldSelectStrategyByStatusWhenNoSuccessfulPayment() {
                // Given
                OrderKey orderKey = new OrderKey("order-6");

                when(paymentRepository.findBy(eq(orderKey), eq(PaymentStatus.SUCCESS)))
                        .thenReturn(Optional.empty()); // 성공한 결제 없음

                // When - 현재 상태가 SUCCESS인 경우
                PaymentCallbackStrategy strategyForSuccess =
                        strategySelector.select(orderKey, PaymentStatus.SUCCESS);

                // When - 현재 상태가 FAILED인 경우
                PaymentCallbackStrategy strategyForFailed =
                        strategySelector.select(orderKey, PaymentStatus.FAILED);

                // Then - 각각 다른 전략 반환
                assertThat(strategyForSuccess).isEqualTo(successfulPaymentStrategy);
                assertThat(strategyForFailed).isEqualTo(failedPaymentStrategy);
            }
        }
    }
}
