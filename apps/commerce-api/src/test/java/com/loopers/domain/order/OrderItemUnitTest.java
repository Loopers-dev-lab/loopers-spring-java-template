package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.loopers.domain.order.dto.OrderItemDomainCreateRequest;

/**
 * OrderItemEntity 단위 테스트
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@DisplayName("OrderItemEntity 단위 테스트")
class OrderItemUnitTest {

    @Nested
    @DisplayName("주문 항목 엔티티 생성")
    class OrderItemCreation {

        @Test
        @DisplayName("유효한 정보로 주문 항목 엔티티를 생성하면 성공한다")
        void should_create_order_item_successfully_with_valid_information() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            );

            // when
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // then
            assertThat(orderItem).isNotNull();
            assertThat(orderItem.getOrderId()).isEqualTo(1L);
            assertThat(orderItem.getProductId()).isEqualTo(100L);
            assertThat(orderItem.getQuantity()).isEqualTo(2);
            assertThat(orderItem.getUnitPrice()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("20000.00"));
        }

        @Test
        @DisplayName("주문 항목 생성 시 총 가격이 자동으로 계산된다")
        void should_calculate_total_price_automatically_when_creating_order_item() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    3,
                    new BigDecimal("15000.00"),
                    null
            );

            // when
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // then
            BigDecimal expectedTotal = new BigDecimal("15000.00").multiply(BigDecimal.valueOf(3));
            assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(expectedTotal);
            assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(new BigDecimal("45000.00"));
        }

        @Test
        @DisplayName("주문 항목 생성 요청이 null인 경우 예외가 발생한다")
        void should_throw_exception_when_create_request_is_null() {
            // given
            OrderItemDomainCreateRequest request = null;

            // when & then
            assertThatThrownBy(() -> OrderItemEntity.createOrderItem(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 항목 생성 요청은 필수입니다.");
        }

        @Test
        @DisplayName("주문 ID가 null인 경우 예외가 발생한다")
        void should_throw_exception_when_order_id_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    null,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 ID는 필수입니다.");
        }

        @Test
        @DisplayName("상품 ID가 null인 경우 예외가 발생한다")
        void should_throw_exception_when_product_id_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    null,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품 ID는 필수입니다.");
        }

        @Test
        @DisplayName("주문 수량이 null인 경우 예외가 발생한다")
        void should_throw_exception_when_quantity_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    null,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 필수입니다.");
        }

        @Test
        @DisplayName("주문 수량이 0 이하인 경우 예외가 발생한다")
        void should_throw_exception_when_quantity_is_zero_or_negative() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    0,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 1 이상이어야 합니다.");

            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    -1,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 1 이상이어야 합니다.");
        }

        @Test
        @DisplayName("주문 수량이 999를 초과하는 경우 예외가 발생한다")
        void should_throw_exception_when_quantity_exceeds_999() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    1000,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 999개를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("단가가 null인 경우 예외가 발생한다")
        void should_throw_exception_when_unit_price_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("단가는 필수입니다.");
        }

        @Test
        @DisplayName("단가가 0 이하인 경우 예외가 발생한다")
        void should_throw_exception_when_unit_price_is_zero_or_negative() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    BigDecimal.ZERO,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("단가는 0보다 커야 합니다.");

            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    new BigDecimal("-1000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("단가는 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("항목 총액 계산")
    class ItemTotalCalculation {

        @Test
        @DisplayName("단가와 수량을 곱한 값이 총 가격으로 계산된다")
        void should_calculate_total_price_by_multiplying_unit_price_and_quantity() {
            // given
            BigDecimal unitPrice = new BigDecimal("25000.00");
            Integer quantity = 4;
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    quantity,
                    unitPrice,
                    null
            );

            // when
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);
            BigDecimal calculatedTotal = orderItem.calculateItemTotal();

            // then
            BigDecimal expectedTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            assertThat(calculatedTotal).isEqualByComparingTo(expectedTotal);
            assertThat(calculatedTotal).isEqualByComparingTo(new BigDecimal("100000.00"));
        }

        @Test
        @DisplayName("수량이 1인 경우 총 가격은 단가와 같다")
        void should_equal_unit_price_when_quantity_is_one() {
            // given
            BigDecimal unitPrice = new BigDecimal("50000.00");
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    1,
                    unitPrice,
                    null
            );

            // when
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // then
            assertThat(orderItem.getTotalPrice()).isEqualByComparingTo(unitPrice);
            assertThat(orderItem.calculateItemTotal()).isEqualByComparingTo(unitPrice);
        }

        @Test
        @DisplayName("수량이 여러 개인 경우 총 가격이 정확히 계산된다")
        void should_calculate_total_price_accurately_with_multiple_quantities() {
            // given
            OrderItemDomainCreateRequest request1 = new OrderItemDomainCreateRequest(
                    1L, 100L,
                    null, 5, new BigDecimal("12000.00"),
                    null
            );
            OrderItemDomainCreateRequest request2 = new OrderItemDomainCreateRequest(
                    1L, 101L, null, 10, new BigDecimal("8500.00"),
                    null
            );
            OrderItemDomainCreateRequest request3 = new OrderItemDomainCreateRequest(
                    1L, 102L, null, 999, new BigDecimal("100.00"),
                    null
            );

            // when
            OrderItemEntity orderItem1 = OrderItemEntity.createOrderItem(request1);
            OrderItemEntity orderItem2 = OrderItemEntity.createOrderItem(request2);
            OrderItemEntity orderItem3 = OrderItemEntity.createOrderItem(request3);

            // then
            assertThat(orderItem1.getTotalPrice()).isEqualByComparingTo(new BigDecimal("60000.00"));
            assertThat(orderItem2.getTotalPrice()).isEqualByComparingTo(new BigDecimal("85000.00"));
            assertThat(orderItem3.getTotalPrice()).isEqualByComparingTo(new BigDecimal("99900.00"));
        }
    }

    @Nested
    @DisplayName("엔티티 검증")
    class EntityValidation {

        @Test
        @DisplayName("모든 필수 값이 유효하면 검증에 성공한다")
        void should_pass_validation_when_all_required_fields_are_valid() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    5,
                    new BigDecimal("20000.00"),
                    null
            );

            // when
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // then - guard() 메서드가 예외를 던지지 않으면 검증 성공
            assertThat(orderItem).isNotNull();
            assertThat(orderItem.getOrderId()).isNotNull();
            assertThat(orderItem.getProductId()).isNotNull();
            assertThat(orderItem.getQuantity()).isNotNull().isPositive();
            assertThat(orderItem.getUnitPrice()).isNotNull().isPositive();
            assertThat(orderItem.getTotalPrice()).isNotNull().isPositive();
        }

        @Test
        @DisplayName("주문 ID가 null이면 검증에 실패한다")
        void should_fail_validation_when_order_id_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    null,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 ID는 필수입니다.");
        }

        @Test
        @DisplayName("상품 ID가 null이면 검증에 실패한다")
        void should_fail_validation_when_product_id_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    null,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품 ID는 필수입니다.");
        }

        @Test
        @DisplayName("주문 수량이 null이면 검증에 실패한다")
        void should_fail_validation_when_quantity_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    null,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 필수입니다.");
        }

        @Test
        @DisplayName("주문 수량이 0 이하이면 검증에 실패한다")
        void should_fail_validation_when_quantity_is_zero_or_negative() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    0,
                    new BigDecimal("10000.00"),
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주문 수량은 1 이상이어야 합니다.");
        }

        @Test
        @DisplayName("단가가 null이면 검증에 실패한다")
        void should_fail_validation_when_unit_price_is_null() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("단가는 필수입니다.");
        }

        @Test
        @DisplayName("단가가 0 이하이면 검증에 실패한다")
        void should_fail_validation_when_unit_price_is_zero_or_negative() {
            // given & when & then
            assertThatThrownBy(() -> new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    BigDecimal.ZERO,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("단가는 0보다 커야 합니다.");
        }

        @Test
        @DisplayName("총 가격이 null이면 검증에 실패한다")
        void should_fail_validation_when_total_price_is_null() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            );
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // when - 리플렉션을 사용하여 totalPrice를 null로 설정
            try {
                java.lang.reflect.Field totalPriceField = OrderItemEntity.class.getDeclaredField("totalPrice");
                totalPriceField.setAccessible(true);
                totalPriceField.set(orderItem, null);

                // then - guard() 메서드 호출 시 예외 발생
                assertThatThrownBy(() -> {
                    java.lang.reflect.Method guardMethod = OrderItemEntity.class.getDeclaredMethod("guard");
                    guardMethod.setAccessible(true);
                    guardMethod.invoke(orderItem);
                })
                        .getCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("총 가격은 0 이상이어야 합니다.");
            } catch (Exception e) {
                throw new RuntimeException("리플렉션 테스트 실패", e);
            }
        }

        @Test
        @DisplayName("총 가격이 0 이하이면 검증에 실패한다")
        void should_fail_validation_when_total_price_is_zero_or_negative() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            );
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // when - 리플렉션을 사용하여 totalPrice를 0으로 설정
            try {
                java.lang.reflect.Field totalPriceField = OrderItemEntity.class.getDeclaredField("totalPrice");
                totalPriceField.setAccessible(true);
                totalPriceField.set(orderItem, BigDecimal.ZERO);

                // then - guard() 메서드 호출 시 예외 발생
                assertThatThrownBy(() -> {
                    java.lang.reflect.Method guardMethod = OrderItemEntity.class.getDeclaredMethod("guard");
                    guardMethod.setAccessible(true);
                    guardMethod.invoke(orderItem);
                })
                        .getCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("총 가격 정합성 오류");
            } catch (Exception e) {
                throw new RuntimeException("리플렉션 테스트 실패", e);
            }
        }

        @Test
        @DisplayName("총 가격이 단가_곱하기_수량과_일치하지_않으면_검증에_실패한다")
        void should_fail_validation_when_total_price_does_not_match_unit_price_times_quantity() {
            // given
            OrderItemDomainCreateRequest request = new OrderItemDomainCreateRequest(
                    1L,
                    100L,
                    null,
                    2,
                    new BigDecimal("10000.00"),
                    null
            );
            OrderItemEntity orderItem = OrderItemEntity.createOrderItem(request);

            // when - 리플렉션을 사용하여 totalPrice를 잘못된 값으로 설정
            try {
                java.lang.reflect.Field totalPriceField = OrderItemEntity.class.getDeclaredField("totalPrice");
                totalPriceField.setAccessible(true);
                totalPriceField.set(orderItem, new BigDecimal("15000.00")); // 잘못된 총액

                // then - guard() 메서드 호출 시 예외 발생
                assertThatThrownBy(() -> {
                    java.lang.reflect.Method guardMethod = OrderItemEntity.class.getDeclaredMethod("guard");
                    guardMethod.setAccessible(true);
                    guardMethod.invoke(orderItem);
                })
                        .getCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("총 가격 정합성 오류:");
            } catch (Exception e) {
                throw new RuntimeException("리플렉션 테스트 실패", e);
            }
        }
    }
}
