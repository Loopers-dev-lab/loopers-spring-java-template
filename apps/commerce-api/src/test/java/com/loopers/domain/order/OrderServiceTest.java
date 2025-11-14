package com.loopers.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
