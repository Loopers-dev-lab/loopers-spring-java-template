package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.user.UserEntity;
import com.loopers.fixtures.PaymentTestFixture;
import com.loopers.fixtures.UserTestFixture;

/**
 * PaymentEntity 단위 테스트
 * <p>
 * Level 1: 단위 테스트 (P0 우선순위)
 * - PaymentEntity의 도메인 로직 검증
 * - 상태 전이 검증
 * - 비즈니스 규칙 검증
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@DisplayName("PaymentEntity 단위 테스트")
class PaymentEntityTest {

    @Nested
    @DisplayName("결제 생성")
    class 결제_생성 {

        @Nested
        @DisplayName("성공 케이스")
        class 성공_케이스 {

            @Test
            @DisplayName("유효한 정보로 PENDING 상태 결제를 생성한다")
            void 유효한_정보로_PENDING_상태_결제를_생성한다() {
                // Given
                PaymentDomainDtos.PaymentDomainCreateRequest request = PaymentTestFixture.createPendingDomainRequest();

                // When
                PaymentEntity payment = PaymentEntity.createPayment(request);

                // Then
                assertThat(payment).isNotNull();
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(payment.getAmount()).isEqualByComparingTo(request.amount());
                assertThat(payment.getOrderNumber()).isEqualTo(request.orderNumber());
                assertThat(payment.getUserId()).isEqualTo(request.userId());
                assertThat(payment.getTransactionKey()).isNull(); // PENDING 상태에서는 null
            }

            @Test
            @DisplayName("UserEntity와 PaymentCommand로 PENDING 결제를 생성한다")
            void UserEntity와_PaymentCommand로_PENDING_결제를_생성한다() {
                // Given
                UserEntity user = UserTestFixture.createDefaultUserEntity();
                PaymentCommand command = PaymentTestFixture.createDefaultCommand();

                // When
                PaymentEntity payment = PaymentEntity.createPending(user, command);

                // Then
                assertThat(payment).isNotNull();
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
                assertThat(payment.getAmount()).isEqualByComparingTo(command.amount());
                assertThat(payment.getOrderNumber()).isEqualTo(command.orderNumber());
                assertThat(payment.getUserId()).isEqualTo(user.getId());
                assertThat(payment.getTransactionKey()).isNull();
            }

            @Test
            @DisplayName("UserEntity와 PaymentCommand로 FAILED 결제를 생성한다")
            void UserEntity와_PaymentCommand로_FAILED_결제를_생성한다() {
                // Given
                UserEntity user = UserTestFixture.createDefaultUserEntity();
                PaymentCommand command = PaymentTestFixture.createDefaultCommand();
                String failureReason = "잔액 부족";

                // When
                PaymentEntity payment = PaymentEntity.createFailed(user, command, failureReason);

                // Then
                assertThat(payment).isNotNull();
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                assertThat(payment.getFailureReason()).isEqualTo(failureReason);
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class 실패_케이스 {

            @Test
            @DisplayName("결제 요청이 null이면 예외가 발생한다")
            void 결제_요청이_null이면_예외가_발생한다() {
                // When & Then
                assertThatThrownBy(() -> PaymentEntity.createPayment(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("결제 생성 요청은 null일 수 없습니다");
            }

            @Test
            @DisplayName("주문 ID가 null이면 예외가 발생한다")
            void 주문_ID가_null이면_예외가_발생한다() {
                // Given

                PaymentDomainDtos.PaymentDomainCreateRequest request = new PaymentDomainDtos.PaymentDomainCreateRequest(
                        1L,
                        null, // orderNumber null
                        "TXN_123",
                        "CREDIT",
                        "1234-5678-9012-3456",
                        "http://callback.url",
                        new BigDecimal("10000"),
                        PaymentStatus.PENDING,
                        ZonedDateTime.now(),
                        null
                );

                // When & Then
                assertThatThrownBy(() -> PaymentEntity.createPayment(request))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("주문 ID는 필수입니다");
            }

            @Test
            @DisplayName("결제 금액이 0 이하면 예외가 발생한다")
            void 결제_금액이_0_이하면_예외가_발생한다() {
                // Given
                PaymentDomainDtos.PaymentDomainCreateRequest request = PaymentTestFixture.createDomainRequestWithAmount(
                        BigDecimal.ZERO
                );

                // When & Then
                assertThatThrownBy(() -> PaymentEntity.createPayment(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("결제 금액은 0보다 커야 합니다");
            }

            @Test
            @DisplayName("결제 금액이 음수면 예외가 발생한다")
            void 결제_금액이_음수면_예외가_발생한다() {
                // Given
                PaymentDomainDtos.PaymentDomainCreateRequest request = PaymentTestFixture.createDomainRequestWithAmount(
                        BigDecimal.valueOf(-1500L)
                );

                // When & Then
                assertThatThrownBy(() -> PaymentEntity.createPayment(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("결제 금액은 0보다 커야 합니다");
            }
        }
    }

    @Nested
    @DisplayName("결제 상태 전이")
    class 결제_상태_전이 {

        @Nested
        @DisplayName("완료 처리 (complete)")
        class 완료_처리 {

            @Test
            @DisplayName("PENDING 상태에서 COMPLETED로 전이된다")
            void PENDING_상태에서_COMPLETED로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createPendingPayment();
                payment.updateTransactionKey("TXN_SUCCESS_123");

                // When
                payment.complete();

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
                assertThat(payment.getCompletedAt()).isNotNull();
                assertThat(payment.getCompletedAt()).isBeforeOrEqualTo(ZonedDateTime.now());
            }

            @Test
            @DisplayName("PENDING이 아닌 상태에서 완료 처리 시 예외가 발생한다")
            void PENDING이_아닌_상태에서_완료_처리_시_예외가_발생한다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCompletedPayment();

                // When & Then
                assertThatThrownBy(() -> payment.complete())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("PENDING 상태의 결제만 완료 처리할 수 있습니다");
            }
        }

        @Nested
        @DisplayName("실패 처리 (fail)")
        class 실패_처리 {

            @Test
            @DisplayName("PENDING 상태에서 FAILED로 전이된다")
            void PENDING_상태에서_FAILED로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createPendingPayment();
                String failureReason = "잔액 부족";

                // When
                payment.fail(failureReason);

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
                assertThat(payment.getFailureReason()).isEqualTo(failureReason);
            }

            @Test
            @DisplayName("PENDING이 아닌 상태에서 실패 처리 시 예외가 발생한다")
            void PENDING이_아닌_상태에서_실패_처리_시_예외가_발생한다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCompletedPayment();

                // When & Then
                assertThatThrownBy(() -> payment.fail("실패 사유"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("PENDING 상태의 결제만 실패 처리할 수 있습니다");
            }
        }

        @Nested
        @DisplayName("타임아웃 처리 (timeout)")
        class 타임아웃_처리 {

            @Test
            @DisplayName("PENDING 상태에서 TIMEOUT으로 전이된다")
            void PENDING_상태에서_TIMEOUT으로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createPendingPayment();

                // When
                payment.timeout();

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.TIMEOUT);
                assertThat(payment.getFailureReason()).contains("타임아웃");
            }

            @Test
            @DisplayName("PENDING이 아닌 상태에서 타임아웃 처리 시 예외가 발생한다")
            void PENDING이_아닌_상태에서_타임아웃_처리_시_예외가_발생한다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCompletedPayment();

                // When & Then
                assertThatThrownBy(() -> payment.timeout())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("PENDING 상태의 결제만 타임아웃 처리할 수 있습니다");
            }
        }

        @Nested
        @DisplayName("취소 처리 (cancel)")
        class 취소_처리 {

            @Test
            @DisplayName("COMPLETED 상태에서 CANCEL로 전이된다")
            void COMPLETED_상태에서_CANCEL로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCompletedPayment();

                // When
                payment.cancel();

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCEL);
            }

            @Test
            @DisplayName("COMPLETED가 아닌 상태에서 취소 처리 시 예외가 발생한다")
            void COMPLETED가_아닌_상태에서_취소_처리_시_예외가_발생한다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createPendingPayment();

                // When & Then
                assertThatThrownBy(() -> payment.cancel())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("완료된 결제만 취소할 수 있습니다");
            }
        }

        @Nested
        @DisplayName("환불 처리 (refund)")
        class 환불_처리 {

            @Test
            @DisplayName("COMPLETED 상태에서 REFUNDED로 전이된다")
            void COMPLETED_상태에서_REFUNDED로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCompletedPayment();

                // When
                payment.refund();

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            }

            @Test
            @DisplayName("CANCEL 상태에서 REFUNDED로 전이된다")
            void CANCEL_상태에서_REFUNDED로_전이된다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createCanceledPayment();

                // When
                payment.refund();

                // Then
                assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            }

            @Test
            @DisplayName("COMPLETED/CANCEL이 아닌 상태에서 환불 처리 시 예외가 발생한다")
            void COMPLETED_CANCEL이_아닌_상태에서_환불_처리_시_예외가_발생한다() {
                // Given
                PaymentEntity payment = PaymentTestFixture.createPendingPayment();

                // When & Then
                assertThatThrownBy(() -> payment.refund())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("완료 또는 취소된 결제만 환불할 수 있습니다");
            }
        }
    }

    @Nested
    @DisplayName("트랜잭션 키 업데이트")
    class 트랜잭션_키_업데이트 {

        @Test
        @DisplayName("트랜잭션 키를 업데이트한다")
        void 트랜잭션_키를_업데이트한다() {
            // Given
            PaymentEntity payment = PaymentTestFixture.createPendingPayment();
            String newTransactionKey = "TXN_NEW_123";

            // When
            payment.updateTransactionKey(newTransactionKey);

            // Then
            assertThat(payment.getTransactionKey()).isEqualTo(newTransactionKey);
        }

        @Test
        @DisplayName("null 트랜잭션 키도 업데이트 가능하다")
        void null_트랜잭션_키도_업데이트_가능하다() {
            // Given
            PaymentEntity payment = PaymentTestFixture.builder()
                    .transactionKey("TXN_OLD")
                    .build();

            // When
            payment.updateTransactionKey(null);

            // Then
            assertThat(payment.getTransactionKey()).isNull();
        }
    }

    @Nested
    @DisplayName("guard() 메서드 검증")
    class Guard_메서드_검증 {

        @Test
        @DisplayName("유효한 상태에서는 예외가 발생하지 않는다")
        void 유효한_상태에서는_예외가_발생하지_않는다() {
            // Given & When & Then
            assertThatCode(() -> PaymentTestFixture.createPendingPayment())
                    .doesNotThrowAnyException();
        }

        // Note: guard()는 @PrePersist/@PreUpdate에서 호출되므로
        // JPA 통합 테스트에서 더 상세하게 검증 필요
    }

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class Builder_패턴_테스트 {

        @Test
        @DisplayName("Builder로 PENDING 결제를 생성한다")
        void Builder로_PENDING_결제를_생성한다() {
            // Given & When
            PaymentEntity payment = PaymentTestFixture.builder()
                    .pending()
                    .amount("30000")
                    .orderId(123L)
                    .build();

            // Then
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(payment.getOrderNumber()).isEqualTo(123L);
            assertThat(payment.getTransactionKey()).isNull();
        }

        @Test
        @DisplayName("Builder로 COMPLETED 결제를 생성한다")
        void Builder로_COMPLETED_결제를_생성한다() {
            // Given & When
            PaymentEntity payment = PaymentTestFixture.builder()
                    .completed()
                    .largeAmount()
                    .build();

            // Then
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        }

        @Test
        @DisplayName("Builder로 FAILED 결제를 생성한다")
        void Builder로_FAILED_결제를_생성한다() {
            // Given
            String failureReason = "카드 한도 초과";

            // When
            PaymentEntity payment = PaymentTestFixture.builder()
                    .failed(failureReason)
                    .smallAmount()
                    .build();

            // Then
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailureReason()).isEqualTo(failureReason);
            assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }
    }
}

