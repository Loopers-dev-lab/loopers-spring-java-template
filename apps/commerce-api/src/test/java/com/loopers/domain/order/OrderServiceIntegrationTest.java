package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderJpaRepository orderJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private PointJpaRepository pointJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;
    @Autowired
    private OrderItemJpaRepository orderItemJpaRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @Test
        void 유저_PK_ID가_주어지면_정상_주문이_생성된다() {
            // arrange
            Long userId = 1L;

            // act
            Order sut = orderService.createOrder(userId);

            // assert
            assertThat(sut.getId()).isEqualTo(1L);
        }

        @Test
        void ORDER_ITEM가_LIST로_저장된다() {
            // arrange
            Integer quantity = 10;
            Integer productPrice = 10000;
            Long productId = 1L;
            Long orderId = 1L;

            OrderItem orderItem1 = OrderItem.create(quantity, productPrice, productId, orderId);
            OrderItem orderItem2 = OrderItem.create(quantity, productPrice, productId, orderId);

            // act
            List<OrderItem> sut = orderService.createOrderItems(List.of(orderItem1, orderItem2));

            // assert
            assertThat(sut).hasSize(2);
        }

    }
}
