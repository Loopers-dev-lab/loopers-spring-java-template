package com.loopers.domain.order;

import com.loopers.application.order.CreateOrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * packageName : com.loopers.domain.order
 * fileName     : OrderServiceIntegrationTest
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
@SpringBootTest
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("주문 생성 성공")
    class OrderCreateSuccess {

        @Test
        @Transactional
        void createOrder_success() {

            // given
            Product p1 = productRepository.save(Product.create(1L, "아메리카노", 3000L, 100L));
            Product p2 = productRepository.save(Product.create(1L, "라떼", 4000L, 200L));

            pointRepository.save(Point.create("user1", 20000L));

            CreateOrderCommand command = new CreateOrderCommand(
                    "user1",
                    List.of(
                            new OrderItemCommand(p1.getId(), 2L),  // 6000원
                            new OrderItemCommand(p2.getId(), 1L)   // 4000원
                    )
            );

            // when
            OrderInfo info = orderFacade.createOrder(command);

            // then
            Order saved = orderRepository.findById(info.orderId()).orElseThrow();

            assertThat(saved.getStatus()).isEqualTo(OrderStatus.COMPLETE);
            assertThat(saved.getTotalAmount()).isEqualTo(10000L);
            assertThat(saved.getOrderItems()).hasSize(2);

            // 재고 감소 확인
            Product updated1 = productRepository.findById(p1.getId()).get();
            Product updated2 = productRepository.findById(p2.getId()).get();
            assertThat(updated1.getStock()).isEqualTo(98);
            assertThat(updated2.getStock()).isEqualTo(199);

            // 포인트 감소 확인
            Point point = pointRepository.findByUserId("user1").get();
            assertThat(point.getBalance()).isEqualTo(10000L);  // 20000 - 10000

        }
    }

    @Nested
    @DisplayName("주문 실패 케이스")
    class OrderCreateFail {

        @Test
        @Transactional
        @DisplayName("재고 부족으로 실패")
        void insufficientStock_fail() {
            Product item = productRepository.save(Product.create(1L, "상품", 1000L, 1L));
            pointRepository.save(Point.create("user1", 5000L));

            CreateOrderCommand command = new CreateOrderCommand(
                    "user1",
                    List.of(new OrderItemCommand(item.getId(), 5L))
            );

            assertThatThrownBy(() -> orderFacade.createOrder(command))
                    .isInstanceOf(RuntimeException.class); // 너의 도메인 예외 타입 맞춰도 됨
        }

        @Test
        @Transactional
        @DisplayName("포인트 부족으로 실패")
        void insufficientPoint_fail() {
            Product item = productRepository.save(Product.create(1L, "상품", 1000L, 10L));
            pointRepository.save(Point.create("user1", 2000L)); // 부족

            CreateOrderCommand command = new CreateOrderCommand(
                    "user1",
                    List.of(new OrderItemCommand(item.getId(), 5L)) // 총 5000원
            );

            assertThatThrownBy(() -> orderFacade.createOrder(command))
                    .hasMessageContaining("포인트"); // 메시지 맞추면 더 정확하게 가능
        }

        @Test
        @Transactional
        @DisplayName("없는 상품 주문 시 실패")
        void noProduct_fail() {
            pointRepository.save(Point.create("user1", 10000L));

            CreateOrderCommand command = new CreateOrderCommand(
                    "user1",
                    List.of(new OrderItemCommand(999L, 1L))
            );

            assertThatThrownBy(() -> orderFacade.createOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Transactional
        @DisplayName("유저 포인트 정보 없으면 실패")
        void noUserPoint_fail() {
            Product item = productRepository.save(Product.create(1L, "상품", 1000L, 10L));

            CreateOrderCommand command = new CreateOrderCommand(
                    "user1",
                    List.of(new OrderItemCommand(item.getId(), 1L))
            );

            assertThatThrownBy(() -> orderFacade.createOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
