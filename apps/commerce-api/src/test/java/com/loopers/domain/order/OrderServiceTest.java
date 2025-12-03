package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.point.PointService;
import com.loopers.support.error.CoreException;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OrderService 테스트")
class OrderServiceTest {

    private ProductRepository productRepository;
    private UserRepository userRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        orderRepository = mock(OrderRepository.class);
        pointService = mock(PointService.class);
        orderService = new OrderService(productRepository, userRepository, orderRepository, pointService);
    }

    @Nested
    @DisplayName("주문 생성")
    class CreateOrder {

        @Test
        @DisplayName("성공 - 정상적인 주문 생성")
        void createOrder_Success() {
            // given
            String userId = "user123";
            User user = User.create(userId, "test@example.com", "1990-01-01", "MALE");

            Product product1 = createProduct(1L, "상품1", new BigDecimal("10000"), 100);
            Product product2 = createProduct(2L, "상품2", new BigDecimal("5000"), 50);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
            when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Order.reconstruct(
                        1L,
                        order.getUserId(),
                        order.getOrderItems(),
                        order.getTotalAmount(),
                        order.getUsedPoints(),
                        order.getFinalAmount(),
                        order.getStatus(),
                        order.getOrderedAt(),
                        order.getModifiedAt()
                );
            });

            var orderItems = Arrays.asList(
                    new OrderService.OrderItemRequest(1L, 2),
                    new OrderService.OrderItemRequest(2L, 1)
            );

            // when
            Order result = orderService.createOrder(userId, orderItems, 1000);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("25000")); // 10000*2 + 5000*1
            assertThat(result.getUsedPoints()).isEqualTo(1000);
            assertThat(result.getFinalAmount()).isEqualTo(new BigDecimal("24000"));

            // 포인트 차감 확인: PointService 사용
            verify(pointService).consume(userId, 1000L);

            // 재고 차감 확인
            verify(productRepository, times(1)).save(product1);
            verify(productRepository, times(1)).save(product2);
            assertThat(product1.getStock()).isEqualTo(98);
            assertThat(product2.getStock()).isEqualTo(49);

            // 주문 저장 확인
            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void createOrder_UserNotFound() {
            // given
            when(userRepository.findById("unknown-user")).thenReturn(Optional.empty());

            var orderItems = Collections.singletonList(
                    new OrderService.OrderItemRequest(1L, 1)
            );

            // when & then
            assertThatThrownBy(() -> orderService.createOrder("unknown-user", orderItems, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 사용자입니다: unknown-user");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 상품")
        void createOrder_ProductNotFound() {
            // given
            String userId = "user123";
            User user = User.create(userId, "test@example.com", "1990-01-01", "MALE");

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            var orderItems = Collections.singletonList(
                    new OrderService.OrderItemRequest(999L, 1)
            );

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(userId, orderItems, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품을 찾을 수 없습니다: 999");
        }

        @Test
        @DisplayName("실패 - 재고 부족")
        void createOrder_InsufficientStock() {
            // given
            String userId = "user123";
            User user = User.create(userId, "test@example.com", "1990-01-01", "MALE");
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 5); // 재고 5개

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            var orderItems = Collections.singletonList(
                    new OrderService.OrderItemRequest(1L, 10) // 10개 주문
            );

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(userId, orderItems, 0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }

        @Test
        @DisplayName("실패 - 포인트 부족")
        void createOrder_InsufficientPoints() {
            // given
            String userId = "user123";
            User user = User.create(userId, "test@example.com", "1990-01-01", "MALE");
            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 100);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));

            var orderItems = Collections.singletonList(
                    new OrderService.OrderItemRequest(1L, 1)
            );

            // when & then
            // 포인트 소진 시 부족 예외는 PointService에서 발생하도록 스텁
            doThrow(new CoreException(com.loopers.support.error.ErrorType.BAD_REQUEST, "포인트가 부족합니다.")
            ).when(pointService).consume(userId, 1000L);

            assertThatThrownBy(() -> orderService.createOrder(userId, orderItems, 1000))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("포인트가 부족합니다.");
        }
    }

    @Nested
    @DisplayName("주문 취소")
    class CancelOrder {

        @Test
        @DisplayName("성공 - 정상적인 주문 취소")
        void cancelOrder_Success() {
            // given
            String userId = "user123";
            Long orderId = 1L;

            User user = User.create(userId, "test@example.com", "1990-01-01", "MALE");

            Product product = createProduct(1L, "상품1", new BigDecimal("10000"), 98);
            // 주문 시 재고가 100 -> 98로 차감됨

            var orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 2)
            );
            Order order = Order.reconstruct(
                    orderId,
                    userId,
                    orderItems,
                    new BigDecimal("20000"),
                    1000,
                    new BigDecimal("19000"),
                    OrderStatus.PENDING,
                    null,
                    null
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // when
            orderService.cancelOrder(orderId, userId);

            // then
            assertThat(order.isCancelled()).isTrue();

            // 재고 복구 확인 (98 + 2 = 100)
            verify(productRepository, times(1)).save(product);
            assertThat(product.getStock()).isEqualTo(100);

            // 포인트 복구 확인: PointService 사용
            verify(pointService).refund(userId, 1000L);

            // 주문 저장 확인
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void cancelOrder_OrderNotFound() {
            // given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(999L, "user123"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다: 999");
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 주문 취소 시도")
        void cancelOrder_NotOwner() {
            // given
            Long orderId = 1L;
            var orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );
            Order order = Order.reconstruct(
                    orderId,
                    "user123",
                    orderItems,
                    new BigDecimal("10000"),
                    0,
                    new BigDecimal("10000"),
                    OrderStatus.PENDING,
                    null,
                    null
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, "other-user"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("본인의 주문만 취소할 수 있습니다");
        }

        @Test
        @DisplayName("실패 - 이미 완료된 주문 취소 시도")
        void cancelOrder_AlreadyCompleted() {
            // given
            Long orderId = 1L;
            var orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );
            Order order = Order.reconstruct(
                    orderId,
                    "user123",
                    orderItems,
                    new BigDecimal("10000"),
                    0,
                    new BigDecimal("10000"),
                    OrderStatus.COMPLETED, // 완료 상태
                    null,
                    null
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, "user123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("완료된 주문은 취소할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("주문 완료")
    class CompleteOrder {

        @Test
        @DisplayName("성공 - 주문 완료 처리")
        void completeOrder_Success() {
            // given
            Long orderId = 1L;
            var orderItems = Collections.singletonList(
                    OrderItem.create(1L, "상품1", new BigDecimal("10000"), 1)
            );
            Order order = Order.reconstruct(
                    orderId,
                    "user123",
                    orderItems,
                    new BigDecimal("10000"),
                    0,
                    new BigDecimal("10000"),
                    OrderStatus.PENDING,
                    null,
                    null
            );

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // when
            orderService.completeOrder(orderId);

            // then
            assertThat(order.isCompleted()).isTrue();
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void completeOrder_OrderNotFound() {
            // given
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 주문입니다: 999");
        }
    }

    // === Helper Methods ===

    private Product createProduct(Long id, String name, BigDecimal price, int stock) {
        return Product.reconstitute(
                id,
                name,
                "테스트 상품",
                price,
                stock,
                "https://example.com/image.jpg",
                1L,
                ProductStatus.ACTIVE,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );
    }
}
