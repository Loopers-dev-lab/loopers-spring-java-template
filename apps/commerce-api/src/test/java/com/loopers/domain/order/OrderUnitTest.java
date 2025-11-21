package com.loopers.domain.order;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

/**
 * OrderEntity 단위 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@DisplayName("OrderEntity 단위 테스트")
class OrderUnitTest {

    @Nested
    @DisplayName("주문 엔티티 생성")
    class 주문_엔티티_생성 {

        @Test
        @DisplayName("유효한 정보로 주문 엔티티를 생성하면 성공한다")
        void should_create_order_entity_successfully_with_valid_information() {
            // given
            Long userId = 1L;
            BigDecimal originalTotalAmount = new BigDecimal("50000");
            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal finalTotalAmount = new BigDecimal("50000");
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(userId, originalTotalAmount, discountAmount, finalTotalAmount);

            // when
            OrderEntity order = OrderEntity.createOrder(request);

            // then
            Assertions.assertThat(order).isNotNull();
            Assertions.assertThat(order.getUserId()).isEqualTo(userId);
            Assertions.assertThat(order.getOriginalTotalAmount()).isEqualByComparingTo(originalTotalAmount);
            Assertions.assertThat(order.getDiscountAmount()).isEqualByComparingTo(discountAmount);
            Assertions.assertThat(order.getFinalTotalAmount()).isEqualByComparingTo(finalTotalAmount);
        }

        @Test
        @DisplayName("주문 생성 시 상태는 PENDING이다")
        void should_have_pending_status_when_order_is_created() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));

            // when
            OrderEntity order = OrderEntity.createOrder(request);

            // then
            Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            Assertions.assertThat(order.isPending()).isTrue();
        }

        @Test
        @DisplayName("주문 생성 요청이 null인 경우 예외가 발생한다")
        void should_throw_exception_when_create_request_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() -> OrderEntity.createOrder(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("주문 생성 요청은 null일 수 없습니다.");
        }

        @Test
        @DisplayName("사용자 ID가 null인 경우 예외가 발생한다")
        void should_throw_exception_when_user_id_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                            new OrderDomainCreateRequest(null, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자 ID는 필수입니다.");
        }

        @Test
        @DisplayName("주문 총액이 null인 경우 예외가 발생한다")
        void should_throw_exception_when_total_amount_is_null() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                            new OrderDomainCreateRequest(1L, null, BigDecimal.ZERO, new BigDecimal("50000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인 전 총액은 필수입니다.");
        }

        @Test
        @DisplayName("주문 총액이 0 이하인 경우 예외가 발생한다")
        void should_throw_exception_when_total_amount_is_zero_or_negative() {
            // given & when & then
            Assertions.assertThatThrownBy(() ->
                            new OrderDomainCreateRequest(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인 전 총액은 0보다 커야 합니다.");

            Assertions.assertThatThrownBy(() ->
                            new OrderDomainCreateRequest(1L, new BigDecimal("-1000"), BigDecimal.ZERO, new BigDecimal("-1000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("할인 전 총액은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("주문 확정")
    class 주문_확정 {
        private OrderEntity order;

        @BeforeEach
        void setUp() {
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            order = OrderEntity.createOrder(request);
        }

        @Test
        @DisplayName("PENDING 상태의 주문을 확정하면 CONFIRMED 상태로 변경된다")
        void should_change_status_to_confirmed_when_confirming_pending_order() {
            // given
            Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);

            // when
            order.confirmOrder();

            // then
            Assertions.assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            Assertions.assertThat(order.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 확정하면 예외가 발생한다")
        void should_throw_exception_when_confirming_non_pending_order() {
            // given
            order.confirmOrder(); // CONFIRMED 상태로 변경

            // when & then
            Assertions.assertThatThrownBy(() -> order.confirmOrder())
                    .isInstanceOf(CoreException.class)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.INVALID_ORDER_STATUS)
                    .hasMessageContaining("주문 확정은 대기 상태 또는 활성화된 주문만 가능합니다.");
        }
    }

    @Nested
    @DisplayName("주문 상태 확인")
    class 주문_상태_확인 {
        private OrderEntity order;

        @BeforeEach
        void setUp() {
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            order = OrderEntity.createOrder(request);
        }

        @Test
        @DisplayName("PENDING 상태의 주문은 isPending이 true를 반환한다")
        void should_return_true_for_is_pending_when_order_status_is_pending() {
            // given & when & then
            Assertions.assertThat(order.isPending()).isTrue();
            Assertions.assertThat(order.isConfirmed()).isFalse();
        }

        @Test
        @DisplayName("CONFIRMED 상태의 주문은 isConfirmed가 true를 반환한다")
        void should_return_true_for_is_confirmed_when_order_status_is_confirmed() {
            // given
            order.confirmOrder();

            // when & then
            Assertions.assertThat(order.isConfirmed()).isTrue();
            Assertions.assertThat(order.isPending()).isFalse();
        }

        @Test
        @DisplayName("PENDING 상태의 주문은 isConfirmed가 false를 반환한다")
        void should_return_false_for_is_confirmed_when_order_status_is_pending() {
            // given & when & then
            Assertions.assertThat(order.isConfirmed()).isFalse();
        }
    }

    @Nested
    @DisplayName("엔티티 검증")
    class 엔티티_검증 {

        @Test
        @DisplayName("모든 필수 값이 유효하면 검증에 성공한다")
        void should_pass_validation_when_all_required_fields_are_valid() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            OrderEntity order = OrderEntity.createOrder(request);

            // when & then
            Assertions.assertThatCode(order::guard)
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("사용자 ID가 null이면 검증에 실패한다")
        void should_fail_validation_when_user_id_is_null() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            OrderEntity order = OrderEntity.createOrder(request);

            Field userIdField = ReflectionUtils.findField(OrderEntity.class, "userId");
            assertNotNull(userIdField);
            ReflectionUtils.makeAccessible(userIdField);
            ReflectionUtils.setField(userIdField, order, null);

            // when & then
            Assertions.assertThatThrownBy(order::guard)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("사용자 ID는 필수입니다.");
        }

        @Test
        @DisplayName("주문 총액이 null이면 검증에 실패한다")
        void should_fail_validation_when_total_amount_is_null() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            OrderEntity order = OrderEntity.createOrder(request);

            Field originalTotalAmountField = ReflectionUtils.findField(OrderEntity.class, "originalTotalAmount");
            assertNotNull(originalTotalAmountField);
            ReflectionUtils.makeAccessible(originalTotalAmountField);
            ReflectionUtils.setField(originalTotalAmountField, order, null);

            // when & then
            Assertions.assertThatThrownBy(order::guard)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("할인 전 총액은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("주문 총액이 0 이하이면 검증에 실패한다")
        void should_fail_validation_when_total_amount_is_zero_or_negative() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            OrderEntity order = OrderEntity.createOrder(request);

            Field originalTotalAmountField = ReflectionUtils.findField(OrderEntity.class, "originalTotalAmount");
            assertNotNull(originalTotalAmountField);
            ReflectionUtils.makeAccessible(originalTotalAmountField);

            // 0인 경우
            ReflectionUtils.setField(originalTotalAmountField, order, BigDecimal.ZERO);
            Assertions.assertThatThrownBy(order::guard)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("할인 전 총액은 0보다 커야 합니다.");

            // 음수인 경우
            ReflectionUtils.setField(originalTotalAmountField, order, new BigDecimal("-1000"));
            Assertions.assertThatThrownBy(order::guard)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("할인 전 총액은 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("주문 상태가 null이면 검증에 실패한다")
        void should_fail_validation_when_order_status_is_null() {
            // given
            OrderDomainCreateRequest request = new OrderDomainCreateRequest(1L, new BigDecimal("50000"), BigDecimal.ZERO, new BigDecimal("50000"));
            OrderEntity order = OrderEntity.createOrder(request);

            Field statusField = ReflectionUtils.findField(OrderEntity.class, "status");
            assertNotNull(statusField);
            ReflectionUtils.makeAccessible(statusField);
            ReflectionUtils.setField(statusField, order, null);

            // when & then
            Assertions.assertThatThrownBy(order::guard)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("주문 상태는 필수입니다.");
        }
    }
}
