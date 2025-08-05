package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderModelTest {

    @Nested
    @DisplayName("주문 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 주문을 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange

            // act
            OrderModel order = OrderFixture.createOrderModel();

            // assert
            assertAll(
                    () -> assertThat(order).isNotNull(),
                    () -> assertThat(order.getOrderNumber().getValue()).isEqualTo(OrderFixture.ODER_NUMBER),
                    () -> assertThat(order.getUserId().getValue()).isEqualTo(OrderFixture.ODER_USER_ID),
                    () -> assertThat(order.getStatus().getValue()).isEqualTo(OrderFixture.ODER_ORDER_STATUS),
                    () -> assertThat(order.getOrderItems()).isEmpty()
            );
        }

        @DisplayName("사용자 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenUserIdNull() {
            // arrange
            Long userId = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderFixture.createOrderWithUserId(userId);
            });

            assertThat(exception.getMessage()).contains("userId cannot be null");
        }

        @DisplayName("올바르지 않은 주문번호 형식으로 생성에 실패한다")
        @Test
        void create_withInvalidOrderNumber() {
            // arrange
            String invalidOrderNumber = "INVALID-ORDER-NUMBER";
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderFixture.createOrderWithOrderNumber(invalidOrderNumber);
            });

            assertThat(exception.getMessage()).contains("올바르지 않은 주문번호 형식입니다");
        }

        @DisplayName("잘못된 주문 상태로 생성에 실패한다")
        @Test
        void create_withInvalidStatus() {
            // arrange
            String status = "Holde";
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderFixture.createOrderWithOrderStatus(status);
            });
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("음수 총 금액으로 생성에 실패한다")
        @Test
        void create_withNegativeTotalPrice() {
            // arrange
            BigDecimal negativeTotalPrice = new BigDecimal("-1000");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderFixture.createOrderWithOrderPrice(negativeTotalPrice);
            });

            assertThat(exception.getMessage()).contains("총 금액은 0 이상이어야 합니다");
        }
    }

    @Nested
    @DisplayName("주문 아이템 추가 관련 테스트")
    class AddItemTest {

        @DisplayName("주문에 아이템을 추가할 수 있다")
        @Test
        void addItem_withValidValues() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();

            OrderItemModel orderItemModel = OrderItemFixture.createOrderItem(order);
            // act

            order.addItem(orderItemModel.getProductId().getValue(),
                    orderItemModel.getOptionId().getValue(),
                    orderItemModel.getQuantity().getValue(),
                    orderItemModel.getOrderItemPrice().getValue(),
                    orderItemModel.getProductSnapshot().getProductName(),
                    orderItemModel.getProductSnapshot().getOptionName(),
                    orderItemModel.getProductSnapshot().getImageUrl()
                    );

            // assert
            assertAll(
                    () -> assertThat(order.getOrderItems().size()).isEqualTo(1),
                    () -> assertThat(order.getTotalPrice().getValue()).isGreaterThan(BigDecimal.ZERO)
            );
        }

        @DisplayName("아이템 추가 시 총 금액이 재계산된다")
        @Test
        void addItem_recalculatesTotal() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();
            BigDecimal pricePerUnit = new BigDecimal("20000");
            BigDecimal quantity = new BigDecimal(2);
            BigDecimal expectedTotal = pricePerUnit.multiply(quantity);

            // act
            order.addItem(1L, 1L, quantity, pricePerUnit, "Product", "Option", "url");

            // assert
            assertThat(order.getTotalPrice().getValue()).isEqualTo(expectedTotal);
        }
    }

    @Nested
    @DisplayName("주문 상태 변경 관련 테스트")
    class StatusChangeTest {

        @DisplayName("주문을 취소할 수 있다")
        @Test
        void cancel_success() {
            // arrange
            OrderModel order = OrderFixture.createOrderWithStatus("PAYMENT_COMPLETED");

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus().getValue()).isEqualTo("CANCELLED");
        }

        @DisplayName("주문 상태를 업데이트할 수 있다")
        @Test
        void updateStatus_withValidStatus() {
            // arrange
            OrderModel order = OrderFixture.createOrderWithStatus("PENDING_PAYMENT");
            String newStatus = "PAYMENT_COMPLETED";

            // act
            order.updateStatus(newStatus);

            // assert
            assertThat(order.getStatus().getValue()).isEqualTo(newStatus);
        }

        @DisplayName("잘못된 상태로 업데이트 시 실패한다")
        @Test
        void updateStatus_withInvalidStatus() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();
            String invalidStatus = "INVALID_STATUS";

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                order.updateStatus(invalidStatus);
            });

            assertThat(exception.getMessage()).contains("주문 상태는 필수입니다");
        }
    }

    @Nested
    @DisplayName("주문 상태 확인 관련 테스트")
    class StatusCheckTest {

        @DisplayName("결제 완료 상태인 주문은 취소 가능하다")
        @Test
        void canBeCancelled_whenPaymentCompleted() {
            // arrange
            OrderModel order = OrderFixture.createOrderWithStatus("PAYMENT_COMPLETED");

            // act & assert
            assertThat(order.canBeCancelled()).isTrue();
        }

        @DisplayName("만료됨 상태인 주문은 취소 불가능하다")
        @Test
        void canBeCancelled_whenPendingPayment() {
            // arrange
            OrderModel order = OrderFixture.createOrderWithStatus("EXPIRED");

            // act & assert
            assertThat(order.canBeCancelled()).isFalse();
        }

        @DisplayName("결제 대기 상태를 올바르게 확인한다")
        @Test
        void isPendingPayment_check() {
            // arrange
            OrderModel pendingOrder = OrderFixture.createOrderWithStatus("PENDING_PAYMENT");
            OrderModel completedOrder = OrderFixture.createOrderWithStatus("PAYMENT_COMPLETED");

            // act & assert
            assertAll(
                    () -> assertThat(pendingOrder.isPendingPayment()).isTrue(),
                    () -> assertThat(completedOrder.isPendingPayment()).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("주문 소유권 확인 관련 테스트")
    class OwnershipTest {

        @DisplayName("올바른 사용자 ID로 소유권을 확인할 수 있다")
        @Test
        void belongsToUser_withCorrectUserId() {
            // arrange
            Long userId = 1L;
            OrderModel order = OrderFixture.createOrderWithUserId(userId);

            // act & assert
            assertThat(order.belongsToUser(userId)).isTrue();
        }

        @DisplayName("잘못된 사용자 ID로 소유권 확인 시 false를 반환한다")
        @Test
        void belongsToUser_withIncorrectUserId() {
            // arrange
            Long userId = 1L;
            Long differentUserId = 2L;
            OrderModel order = OrderFixture.createOrderWithUserId(userId);

            // act & assert
            assertThat(order.belongsToUser(differentUserId)).isFalse();
        }

        @DisplayName("null 사용자 ID로 소유권 확인 시 false를 반환한다")
        @Test
        void belongsToUser_withNullUserId() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();

            // act & assert
            assertThat(order.belongsToUser(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("총 금액 계산 관련 테스트")
    class TotalCalculationTest {

        @DisplayName("빈 주문의 총 금액은 0이다")
        @Test
        void calculateTotal_withEmptyOrder() {
            // arrange
            OrderModel order = OrderModel.of(
                    OrderFixture.ODER_NUMBER,
                    OrderFixture.ODER_USER_ID,
                    OrderFixture.ODER_ORDER_STATUS,
                    BigDecimal.ZERO,
                    Collections.emptyList()
            );

            // act
            BigDecimal total = order.calculateTotal();

            // assert
            assertThat(total).isEqualTo(BigDecimal.ZERO);
        }

        @DisplayName("아이템이 있는 주문의 총 금액을 계산할 수 있다")
        @Test
        void calculateTotal_withItems() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();

            // act
            BigDecimal total = order.calculateTotal();

            // assert
            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @DisplayName("여러 아이템의 총 금액을 올바르게 계산한다")
        @Test
        void calculateTotal_withMultipleItems() {
            // arrange
            OrderModel order = OrderFixture.createOrderModel();
            
            BigDecimal price1 = new BigDecimal("10000");
            BigDecimal price2 = new BigDecimal("20000");
            BigDecimal quantity1 = new BigDecimal(2);
            BigDecimal quantity2 = new BigDecimal(3);
            
            order.addItem(1L, 1L, quantity1, price1, "Product1", "Option1", "url1");
            order.addItem(2L, 2L, quantity2, price2, "Product2", "Option2", "url2");

            // act
            BigDecimal total = order.calculateTotal();

            // assert
            BigDecimal expectedTotal = price1.multiply(quantity1).add(price2.multiply(quantity2));
            assertThat(total).isEqualByComparingTo(expectedTotal);
        }
    }
}
