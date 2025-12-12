package com.loopers.domain.order;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("주문(Order) Entity 테스트")
public class OrderTest {

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 userId와 주문 항목 리스트로 주문을 생성할 수 있다. (Happy Path)")
        @Test
        void should_createOrder_when_validUserIdAndOrderItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000)),
                    OrderItem.create(2L, "상품2", 1, new Price(20000))
            );

            // act
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);

            // assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOrderItems()).hasSize(2);
            assertThat(order.getOrderItems().get(0).getProductId()).isEqualTo(1L);
            assertThat(order.getOrderItems().get(1).getProductId()).isEqualTo(2L);
        }

        @DisplayName("단일 주문 항목으로 주문을 생성할 수 있다. (Edge Case)")
        @Test
        void should_createOrder_when_singleOrderItem() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );

            // act
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);

            // assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOrderItems()).hasSize(1);
        }

        @DisplayName("빈 주문 항목 리스트로 주문을 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_emptyOrderItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = new ArrayList<>();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Order.create(userId, orderItems, PaymentMethod.POINT));
            assertThat(exception.getMessage()).isEqualTo("주문 항목은 최소 1개 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null 주문 항목 리스트로 주문을 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_nullOrderItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Order.create(userId, orderItems, PaymentMethod.POINT));
            assertThat(exception.getMessage()).isEqualTo("주문 항목은 최소 1개 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null userId로 주문을 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_nullUserId() {
            // arrange
            Long userId = null;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Order.create(userId, orderItems, PaymentMethod.POINT));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("0 이하의 userId로 주문을 생성할 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_invalidUserId() {
            // arrange
            Long userId = 0L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Order.create(userId, orderItems, PaymentMethod.POINT));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("여러 주문 항목으로 주문을 생성할 수 있다. (Edge Case)")
        @Test
        void should_createOrder_when_multipleOrderItems() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000)),
                    OrderItem.create(2L, "상품2", 1, new Price(20000)),
                    OrderItem.create(3L, "상품3", 3, new Price(15000))
            );

            // act
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);

            // assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOrderItems()).hasSize(3);
        }

        @DisplayName("DiscountResult를 사용하여 쿠폰이 적용된 주문을 생성할 수 있다. (Happy Path)")
        @Test
        void should_createOrder_when_discountResultProvided() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000)),
                    OrderItem.create(2L, "상품2", 1, new Price(20000))
            );
            // 원가: 40000원, 할인: 5000원, 최종: 35000원
            DiscountResult discountResult = new DiscountResult(
                    1L, // couponId
                    new Price(40000), // originalPrice
                    new Price(5000), // discountAmount
                    new Price(35000) // finalPrice
            );

            // act
            Order order = Order.create(userId, orderItems, discountResult, PaymentMethod.POINT);

            // assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOrderItems()).hasSize(2);
            assertThat(order.getOriginalPrice().amount()).isEqualTo(40000);
            assertThat(order.getDiscountAmount().amount()).isEqualTo(5000);
            assertThat(order.getFinalPrice().amount()).isEqualTo(35000);
            assertThat(order.getCouponId()).isEqualTo(1L);
        }

        @DisplayName("DiscountResult가 null인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_discountResultIsNull() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            DiscountResult discountResult = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> Order.create(userId, orderItems, discountResult, PaymentMethod.POINT));
            assertThat(exception.getMessage()).isEqualTo("할인 결과는 null일 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("쿠폰 없이 DiscountResult를 사용하여 주문을 생성할 수 있다. (Happy Path)")
        @Test
        void should_createOrder_when_discountResultWithoutCoupon() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            // 원가: 10000원, 할인 없음
            DiscountResult discountResult = new DiscountResult(
                    new Price(10000) // originalPrice만 제공 (쿠폰 없음)
            );

            // act
            Order order = Order.create(userId, orderItems, discountResult, PaymentMethod.POINT);

            // assert
            assertThat(order.getUserId()).isEqualTo(1L);
            assertThat(order.getOriginalPrice().amount()).isEqualTo(10000);
            assertThat(order.getDiscountAmount().amount()).isEqualTo(0);
            assertThat(order.getFinalPrice().amount()).isEqualTo(10000);
            assertThat(order.getCouponId()).isNull();
        }

    }

    @DisplayName("주문 조회를 할 때, ")
    @Nested
    class Retrieve {
        @DisplayName("생성한 주문의 userId를 조회할 수 있다. (Happy Path)")
        @Test
        void should_retrieveUserId_when_orderCreated() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);

            // act
            Long retrievedUserId = order.getUserId();

            // assert
            assertThat(retrievedUserId).isEqualTo(1L);
        }

        @DisplayName("생성한 주문의 주문 항목 리스트를 조회할 수 있다. (Happy Path)")
        @Test
        void should_retrieveOrderItems_when_orderCreated() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000)),
                    OrderItem.create(2L, "상품2", 1, new Price(20000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);

            // act
            List<OrderItem> retrievedOrderItems = order.getOrderItems();

            // assert
            assertThat(retrievedOrderItems).hasSize(2);
            assertThat(retrievedOrderItems.get(0).getProductId()).isEqualTo(1L);
            assertThat(retrievedOrderItems.get(1).getProductId()).isEqualTo(2L);
        }
    }

    @DisplayName("재결제 시도 횟수 관리")
    @Nested
    class RetryCount {
        @DisplayName("초기 주문은 재시도 가능 상태이다. (Happy Path)")
        @Test
        void should_beRetryable_when_initialOrder() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.PG);

            // act & assert
            assertThat(order.canRetryPayment()).isTrue();
            assertThat(order.getRetryCount()).isEqualTo(0); // 초기값은 0
        }

        @DisplayName("재시도 횟수를 증가시킬 수 있다. (Happy Path)")
        @Test
        void should_incrementRetryCount_when_called() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.PG);

            // act
            order.incrementRetryCount();

            // assert
            assertThat(order.getRetryCount()).isEqualTo(1);
            assertThat(order.canRetryPayment()).isTrue(); // 1회 증가 후에도 재시도 가능 (최대 2회)
        }

        @DisplayName("재시도 횟수가 2회 이상이면 재시도 불가능하다. (Edge Case)")
        @Test
        void should_notBeRetryable_when_retryCountExceeded() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.PG);
            order.incrementRetryCount(); // 1회
            order.incrementRetryCount(); // 2회

            // act & assert
            assertThat(order.getRetryCount()).isEqualTo(2);
            assertThat(order.canRetryPayment()).isFalse(); // 2회 이상이면 재시도 불가능
        }

        @DisplayName("타임아웃으로 인한 실패 처리를 할 수 있다. (Happy Path)")
        @Test
        void should_markAsFailedByTimeout_when_called() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.PG);
            order.setPaymentInfo("SAMSUNG", "1234-5678-9012-3456", "http://callback.url");

            // act
            order.markAsFailedByTimeout("결제 요청 타임아웃");

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(order.getPgPaymentReason()).isEqualTo("결제 요청 타임아웃");
        }

        @DisplayName("이미 결제 완료된 주문은 타임아웃 실패 처리할 수 없다. (Exception)")
        @Test
        void should_throwException_when_markAsFailedForPaidOrder() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems, PaymentMethod.POINT);
            order.markPaidByPoint();

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                order.markAsFailedByTimeout("타임아웃");
            });
            assertThat(exception.getMessage()).isEqualTo("이미 결제 완료된 주문은 실패 처리할 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
