package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService service;

    static final String USER_ID = "user123";
    static final Long PRODUCT_ID_1 = 1L;
    static final Long PRODUCT_ID_2 = 2L;

    @Nested
    @DisplayName("정상 주문 흐름")
    class NormalOrderFlow {

        @Test
        @DisplayName("단일 상품 주문 성공")
        void orderService1() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 2L, 10_000);
            List<OrderItem> items = List.of(item);
            int totalAmount = 20_000;

            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = service.createOrder(USER_ID, items, totalAmount);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(result.getTotalAmount()).isEqualTo(totalAmount);

            verify(orderRepository).save(any(Order.class));
        }

        @Test
        @DisplayName("여러 상품 주문 성공")
        void orderService2() {
            OrderItem item1 = OrderItem.create(PRODUCT_ID_1, "상품1", 2L, 10_000);
            OrderItem item2 = OrderItem.create(PRODUCT_ID_2, "상품2", 1L, 20_000);
            List<OrderItem> items = List.of(item1, item2);
            int totalAmount = 40_000;

            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            Order result = service.createOrder(USER_ID, items, totalAmount);

            assertThat(result).isNotNull();
            assertThat(result.getOrderItems()).hasSize(2);
            assertThat(result.getTotalAmount()).isEqualTo(totalAmount);

            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    @DisplayName("주문 실패 흐름")
    class OrderFailure {

        @Test
        @DisplayName("이미 확정된 주문을 다시 확정하려고 하면 실패한다")
        void orderService3() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            List<OrderItem> items = List.of(item);
            int totalAmount = 10_000;

            Order order = Order.create(USER_ID, items, totalAmount);
            order.confirm();

            assertThatThrownBy(order::confirm)
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("PENDING 상태에서만 확정할 수 있습니다");
        }

        @Test
        @DisplayName("포인트 부족 시 주문이 저장되지 않는다")
        void orderService4() {
            OrderItem item = OrderItem.create(PRODUCT_ID_1, "상품1", 1L, 10_000);
            List<OrderItem> items = List.of(item);
            int totalAmount = 10_000;

            when(orderRepository.save(any(Order.class)))
                    .thenThrow(new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다."));

            assertThatThrownBy(() -> service.createOrder(USER_ID, items, totalAmount))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }
}
