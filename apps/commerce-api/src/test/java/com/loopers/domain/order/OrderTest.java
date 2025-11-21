package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private User dummyUser;
    private Product dummyProduct1;
    private Product dummyProduct2;

    @BeforeEach
    void setUp() {
        // 테스트에 필요한 User, Product (및 Brand) 더미 객체 생성
        dummyUser = User.create("testuser", "test@mail.com", "1990-01-01", Gender.MALE);
        Brand dummyBrand = Brand.create("Dummy Brand");
        dummyProduct1 = Product.create("Product A", 1000L, 10, dummyBrand);
        dummyProduct2 = Product.create("Product B", 2000L, 10, dummyBrand);
    }

    @Nested
    @DisplayName("주문 생성 (Order.create)")
    class CreateOrder {

        @DisplayName("사용자로 주문을 생성할 수 있다.")
        @Test
        void createOrder() {
            // act
            Order order = Order.create(dummyUser);

            // assert
            assertThat(order.getUser()).isEqualTo(dummyUser);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getTotalAmountValue()).isZero();
            assertThat(order.getOrderItems()).isEmpty();
            assertThat(order.getPaidAt()).isNull();
        }

        @DisplayName("사용자가 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenUserIsNull() {
            // act & assert
            assertThatThrownBy(() -> Order.create(null))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("주문 항목 추가 (addOrderItem)")
    class AddItem {

        @DisplayName("주문에 항목을 추가하면 총 금액이 재계산된다.")
        @Test
        void addOrderItem() {
            // arrange
            Order order = Order.create(dummyUser);

            // act
            order.addOrderItem(dummyProduct1, 2); // 1000 * 2 = 2000
            order.addOrderItem(dummyProduct2, 1); // 2000 * 1 = 2000

            // assert
            assertThat(order.getOrderItems()).hasSize(2);
            assertThat(order.getTotalAmountValue()).isEqualTo(4000L);
            assertThat(order.getOrderItems().get(0).getUnitPriceValue()).isEqualTo(1000L); // 가격 스냅샷 확인
        }

        @DisplayName("상품이 null이면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenProductIsNull() {
            // arrange
            Order order = Order.create(dummyUser);

            // act & assert
            assertThatThrownBy(() -> order.addOrderItem(null, 1))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("수량이 0 이하면 BAD_REQUEST 예외가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void throwsException_whenQuantityIsInvalid(int invalidQuantity) {
            // arrange
            Order order = Order.create(dummyUser);

            // act & assert
            assertThatThrownBy(() -> order.addOrderItem(dummyProduct1, invalidQuantity))
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("결제 완료 (completePayment)")
    class CompletePayment {

        @DisplayName("주문 항목이 존재하면 결제를 완료할 수 있다.")
        @Test
        void completePayment() {
            // arrange
            Order order = Order.create(dummyUser);
            order.addOrderItem(dummyProduct1, 1);
            ZonedDateTime beforePayment = ZonedDateTime.now().minusSeconds(1);

            // act
            order.completePayment();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(order.getPaidAt()).isAfter(beforePayment);
        }

        @DisplayName("주문 항목이 없으면 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenItemsAreEmpty() {
            // arrange
            Order order = Order.create(dummyUser);

            // act & assert
            assertThatThrownBy(order::completePayment)
                    .isInstanceOf(CoreException.class)
                    .extracting(ex -> ((CoreException) ex).getErrorType())
                    .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
