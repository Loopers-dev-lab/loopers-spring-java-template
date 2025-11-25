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
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
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
    private OrderItemJpaRepository orderItemJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private PointJpaRepository pointJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;


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

        @Test
        void 서로_다른_유저_10명이_상품_재고를_1개씩_요청시_재고가_정상적으로_0개로_변경된다() {
            // arrange
            int userCount = 10;

            List<User> users = IntStream.range(0, userCount)
                    .mapToObj(i -> userJpaRepository.save(
                            User.create("user" + i, "user" + i + "@test.com",
                                    LocalDate.of(2020, 1, 1), Gender.MALE)
                    ))
                    .toList();

            users.forEach(user -> {
                Point p = Point.create(user.getId());
                p.charge(100000L);
                pointJpaRepository.save(p);
            });

            Product product = productJpaRepository.save(Product.create("p1", 10_000, new Stock(10), 1L));

            List<OrderCreateItemInfo> items = List.of(
                    new OrderCreateItemInfo(product.getId(), 1)
            );

            // act
            List<CompletableFuture<Void>> futures = IntStream.range(0, userCount)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        Long userId = users.get(i).getId();
                        orderFacade.createOrder(userId, items);
                    }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // assert
            Product sut = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(sut.getStock().getQuantity()).isEqualTo(0);
        }

        @Test
        void 동일_유저가_동시에_여러_주문을_수행해도_포인트가_정상_차감된다() {

            // given
            User user = userJpaRepository.save(User.create("test", "test@test.com",
                    LocalDate.of(2020,1,1), Gender.MALE));

            Point point = Point.create(user.getId());
            point.charge(10000L);
            pointJpaRepository.save(point);

            Product product = productJpaRepository.save(Product.create("p1", 1000, new Stock(10), 1L));
            List<OrderCreateItemInfo> items = List.of(new OrderCreateItemInfo(product.getId(), 1));

            int threadCount = 10;

            // when
            List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
                    .mapToObj(i -> CompletableFuture.runAsync(() -> {
                        try {
                            orderFacade.createOrder(user.getId(), items);
                        } catch (Exception ignored) {}
                    }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // then
            Point sut = pointJpaRepository.findByUserId(user.getId()).orElse(null);
            assertThat(sut.getPoint()).isEqualTo(0L);
        }
    }
}
