package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.order.OrderItemJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderFacadeIntegrationTest {
    @Autowired
    private OrderFacade orderFacade;
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

        @DisplayName("정상 주문 시 주문 / 재고 / 포인트가 정상 처리된다")
        @Test
        void createOrder_success() {
            // arrange
            User user = userJpaRepository.save(User.create("testUser",  "test@test.com", LocalDate.of(2020, 1, 1), Gender.MALE));
            Point point = Point.create(user.getId());
            point.charge(100000L);
            pointJpaRepository.save(point);

            Product p1 = productJpaRepository.save(Product.create("p1", 10_000, new Stock(10), 1L));
            Product p2 = productJpaRepository.save(Product.create("p2", 20_000, new Stock(5), 1L));

            List<OrderCreateItemInfo> items = List.of(
                    new OrderCreateItemInfo(p1.getId(), 2),
                    new OrderCreateItemInfo(p2.getId(), 1)
            );

            // act
            OrderInfo sut = orderFacade.createOrder(user.getId(), items);

            // assert
            assertThat(sut.id()).isEqualTo(user.getId());
            assertThat(sut.orderItemInfos()).hasSize(2);
        }
    }
}
