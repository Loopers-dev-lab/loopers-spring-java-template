package com.loopers.domain.order;

import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DisplayName("주문 서비스(OrderService) 테스트")
public class OrderServiceIntegrationTest {

    @MockitoSpyBean
    private OrderRepository spyOrderRepository;

    @Autowired
    private OrderService orderService;

    @DisplayName("주문을 저장할 때, ")
    @Nested
    class SaveOrder {
        @DisplayName("정상적인 주문을 저장하면 주문이 저장된다. (Happy Path)")
        @Test
        void should_saveOrder_when_validOrder() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000)),
                    OrderItem.create(2L, "상품2", 1, new Price(20000))
            );
            Order order = Order.create(userId, orderItems);
//            when(spyOrderRepository.save(any(Order.class))).thenReturn(order);

            // act
            Order result = orderService.save(order);

            // assert
            verify(spyOrderRepository).save(any(Order.class));
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getOrderItems()).hasSize(2);
        }

        @DisplayName("단일 주문 항목을 가진 주문을 저장할 수 있다. (Edge Case)")
        @Test
        void should_saveOrder_when_singleOrderItem() {
            // arrange
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 1, new Price(10000))
            );
            Order order = Order.create(userId, orderItems);
//            when(spyOrderRepository.save(any(Order.class))).thenReturn(order);

            // act
            Order result = orderService.save(order);

            // assert
            verify(spyOrderRepository).save(any(Order.class));
            assertThat(result).isNotNull();
            assertThat(result.getOrderItems()).hasSize(1);
        }
    }

    @DisplayName("주문 ID와 사용자 ID로 주문을 조회할 때, ")
    @Nested
    class GetOrderByIdAndUserId {
        @DisplayName("존재하는 주문 ID와 사용자 ID로 조회하면 주문을 반환한다. (Happy Path)")
        @Test
        void should_returnOrder_when_orderExists() {
            // arrange
            Long orderId = 1L;
            Long userId = 1L;
            List<OrderItem> orderItems = List.of(
                    OrderItem.create(1L, "상품1", 2, new Price(10000))
            );
            Order order = Order.create(userId, orderItems);
            when(spyOrderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));

            // act
            Order result = orderService.getOrderByIdAndUserId(orderId, userId);

            // assert
            verify(spyOrderRepository).findByIdAndUserId(1L, 1L);
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getOrderItems()).hasSize(1);
        }

        @DisplayName("존재하지 않는 주문 ID로 조회하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_orderNotFound() {
            // arrange
            Long orderId = 999L;
            Long userId = 1L;
            when(spyOrderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.empty());

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.getOrderByIdAndUserId(orderId, userId);
            });

            // assert
            verify(spyOrderRepository).findByIdAndUserId(999L, 1L);
            assertThat(exception.getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("다른 사용자의 주문 ID로 조회하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_orderBelongsToDifferentUser() {
            // arrange
            Long orderId = 1L;
            Long userId = 1L;
            Long differentUserId = 2L;
            when(spyOrderRepository.findByIdAndUserId(orderId, differentUserId)).thenReturn(Optional.empty());

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.getOrderByIdAndUserId(orderId, differentUserId);
            });

            // assert
            verify(spyOrderRepository).findByIdAndUserId(1L, 2L);
            assertThat(exception.getMessage()).isEqualTo("주문을 찾을 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}

