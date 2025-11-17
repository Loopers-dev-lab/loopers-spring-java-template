package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @DisplayName("주문을 생성할 때,")
    @Nested
    class CreateOrder {

        @DisplayName("사용자 ID와 배송 정보가 주어지면 주문이 생성된다.")
        @Test
        void createsOrder_whenUserIdAndDeliveryAreProvided() {
            // arrange
            Long userId = 1L;
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");

            // act
            Order order = orderService.createOrder(userId, delivery);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(userId),
                    () -> assertThat(order.getDelivery()).isEqualTo(delivery),
                    () -> assertThat(order.getTotalPrice()).isEqualTo(0),
                    () -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED),
                    () -> assertThat(order.getOrderItems()).isEmpty()
            );
        }
    }

    @DisplayName("주문 상품을 생성할 때,")
    @Nested
    class CreateOrderItem {

        @DisplayName("주문, 상품, 수량이 주어지면 주문 상품이 생성되고 주문에 추가된다.")
        @Test
        void createsOrderItem_whenOrderProductAndQuantityAreProvided() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);
            Integer price = 10000;
            Product product = createProduct(1L, "상품명", price);
            Integer quantity = 2;

            // act
            orderService.createOrderItem(order, product, quantity);

            // assert
            assertThat(order.getOrderItems()).hasSize(1);
            OrderItem orderItem = order.getOrderItems().get(0);
            assertAll(
                    () -> assertThat(orderItem.getProductId()).isEqualTo(product.getId()),
                    () -> assertThat(orderItem.getProductName()).isEqualTo(product.getName()),
                    () -> assertThat(orderItem.getPrice()).isEqualTo(price),
                    () -> assertThat(orderItem.getQuantity()).isEqualTo(quantity)
            );
        }
    }

    @DisplayName("총 가격을 추가할 때,")
    @Nested
    class AddTotalPrice {

        @DisplayName("가격과 수량이 주어지면 주문의 총 가격에 추가된다.")
        @Test
        void addsTotalPrice_whenPriceAndQuantityAreProvided() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);
            int price = 10000;
            int quantity = 2;

            // act
            orderService.addTotalPrice(order, price, quantity);

            // assert
            assertThat(order.getTotalPrice()).isEqualTo(20000);
        }

        @DisplayName("여러 번 가격을 추가하면 총액이 누적된다.")
        @Test
        void accumulatesTotalPrice_whenPriceIsAddedMultipleTimes() {
            // arrange
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(1L, delivery);

            // act
            orderService.addTotalPrice(order, 10000, 2);
            orderService.addTotalPrice(order, 5000, 3);

            // assert
            assertThat(order.getTotalPrice()).isEqualTo(35000);
        }
    }

    @DisplayName("사용자 ID로 주문 목록을 조회할 때,")
    @Nested
    class FindOrdersByUserId {

        @DisplayName("주문이 존재하면 주문 목록이 반환된다.")
        @Test
        void returnsOrders_whenOrdersExist() {
            // arrange
            Long userId = 1L;
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order1 = Order.createOrder(userId, delivery);
            Order order2 = Order.createOrder(userId, delivery);
            List<Order> orders = List.of(order1, order2);

            doReturn(orders).when(orderRepository).findOrdersByUserId(userId);

            // act
            List<Order> result = orderService.findOrdersByUserId(userId);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(order1, order2);
            verify(orderRepository, times(1)).findOrdersByUserId(userId);
        }

        @DisplayName("주문이 없으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenOrdersDoNotExist() {
            // arrange
            Long userId = 1L;
            doReturn(new ArrayList<>()).when(orderRepository).findOrdersByUserId(userId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.findOrdersByUserId(userId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("해당 사용자의 주문 내역이 없습니다.");
            verify(orderRepository, times(1)).findOrdersByUserId(userId);
        }
    }

    @DisplayName("주문 ID와 사용자 ID로 주문을 조회할 때,")
    @Nested
    class FindOrderByIdAndUserId {

        @DisplayName("주문이 존재하면 주문이 반환된다.")
        @Test
        void returnsOrder_whenOrderExists() {
            // arrange
            Long orderId = 1L;
            Long userId = 1L;
            Delivery delivery = Delivery.createDelivery("홍길동", "010-1234-5678", "서울시 강남구", "테헤란로 123");
            Order order = Order.createOrder(userId, delivery);

            doReturn(Optional.of(order)).when(orderRepository).findByIdAndUserId(orderId, userId);

            // act
            Optional<Order> result = orderService.findOrderByIdAndUserId(orderId, userId);

            // assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(order);
            verify(orderRepository, times(1)).findByIdAndUserId(orderId, userId);
        }

        @DisplayName("주문이 없으면 빈 Optional이 반환된다.")
        @Test
        void returnsEmpty_whenOrderDoesNotExist() {
            // arrange
            Long orderId = 1L;
            Long userId = 1L;
            doReturn(Optional.empty()).when(orderRepository).findByIdAndUserId(orderId, userId);

            // act
            Optional<Order> result = orderService.findOrderByIdAndUserId(orderId, userId);

            // assert
            assertThat(result).isEmpty();
            verify(orderRepository, times(1)).findByIdAndUserId(orderId, userId);
        }
    }

    private Product createProduct(Long id, String name, Integer price) {
        Product product = mock(Product.class);
        Price priceValue = mock(Price.class);
        Stock stock = mock(Stock.class);
        
        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getPrice()).thenReturn(priceValue);
        when(priceValue.getPrice()).thenReturn(price);
        when(product.getStock()).thenReturn(stock);
        
        return product;
    }
}
