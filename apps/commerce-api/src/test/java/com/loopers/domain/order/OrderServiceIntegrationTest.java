package com.loopers.domain.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderItemInfo;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 등록할 때,")
    @Nested
    class createOrder {
        @DisplayName("재고 및 유저 포인트가 충분하다면 주문 등록 성공한다.")
        @Test
        void succeeds_whenStockAndPointAreSufficient() {

            // arrange
            User user = createValidUser();
            userRepository.save(user);

            Product product1 = Product.create(1L, "상품이름1", Money.of(1000));
            Product product2 = Product.create(2L, "상품이름2", Money.of(2000));
            productRepository.save(product1);
            productRepository.save(product2);

            stockRepository.save(Stock.create(product1.getId(), 1));
            stockRepository.save(Stock.create(product2.getId(), 1));

            pointRepository.save(Point.create(user.getId(), Money.of(3000)));

            List<OrderItemInfo> itemInfoList = new ArrayList<>();
            itemInfoList.add(OrderItemInfo.of(product1.getId(), 1));
            itemInfoList.add(OrderItemInfo.of(product2.getId(), 1));

            // act
            Order result = orderFacade.createOrder(user.getId(), itemInfoList);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(user.getId()),
                () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID),
                () -> assertThat(result.getItems().get(0).getProductId()).isEqualTo(product1.getId()),
                () -> assertThat(result.getItems().get(0).getUnitPrice()).isEqualTo(Money.of(1000)),
                () -> assertThat(result.getItems().get(0).getQuantity()).isEqualTo(1),
                () -> assertThat(result.getItems().get(1).getProductId()).isEqualTo(product2.getId()),
                () -> assertThat(result.getItems().get(1).getUnitPrice()).isEqualTo(Money.of(2000)),
                () -> assertThat(result.getItems().get(1).getQuantity()).isEqualTo(1)
            );
        }

        @DisplayName("상품의 재고가 부족한 경우 주문 실패하며 모두 롤백처리된다.")
        @Test
        void fails_whenProductStockIsInsufficient() {

            // assert
            User user = createValidUser();
            userRepository.save(user);

            Product product1 = Product.create(1L, "상품이름1", Money.of(1000));
            Product product2 = Product.create(2L, "상품이름2", Money.of(2000));
            productRepository.save(product1);
            productRepository.save(product2);

            stockRepository.save(Stock.create(product1.getId(), 1));
            stockRepository.save(Stock.create(product2.getId(), 1));

            pointRepository.save(Point.create(user.getId(), Money.of(3000)));

            List<OrderItemInfo> itemInfoList = new ArrayList<>();
            itemInfoList.add(OrderItemInfo.of(product1.getId(), 1));
            itemInfoList.add(OrderItemInfo.of(product2.getId(), 2));

            // act + assert
            assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), itemInfoList))
                    .hasMessageContaining("주문 상품의 재고가 부족합니다.");

            Stock unsavedStock1 = stockRepository.findByProductId(product1.getId()).orElseThrow();
            Stock unsavedStock2 = stockRepository.findByProductId(product2.getId()).orElseThrow();
            Point unsavedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
            List<Order> orders = orderRepository.findAllByUserId(user.getId());

            assertAll(
                    () -> assertThat(unsavedStock1.getQuantity()).isEqualTo(1),
                    () -> assertThat(unsavedStock2.getQuantity()).isEqualTo(1),
                    () -> assertThat(unsavedPoint.getBalance()).isEqualTo(Money.of(3000)),
                    () -> assertThat(orders.isEmpty()).isTrue()
            );
        }

        @DisplayName("유저 포인트가 주문 금액보다 부족한 경우 주문 실패하며 모두 롤백처리된다.")
        @Test
        void fails_whenUserPointIsNotEnough() {
            // assert
            User user = createValidUser();
            userRepository.save(user);

            Product product1 = Product.create(1L, "상품이름1", Money.of(1000));
            Product product2 = Product.create(2L, "상품이름2", Money.of(2000));
            productRepository.save(product1);
            productRepository.save(product2);

            stockRepository.save(Stock.create(product1.getId(), 1));
            stockRepository.save(Stock.create(product2.getId(), 1));

            pointRepository.save(Point.create(user.getId(), Money.of(0)));

            List<OrderItemInfo> itemInfoList = new ArrayList<>();
            itemInfoList.add(OrderItemInfo.of(product1.getId(), 1));
            itemInfoList.add(OrderItemInfo.of(product2.getId(), 1));

            // act + assert
            assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), itemInfoList))
                    .hasMessageContaining("잔여 포인트가 부족합니다.");

            Stock unsavedStock1 = stockRepository.findByProductId(product1.getId()).orElseThrow();
            Stock unsavedStock2 = stockRepository.findByProductId(product2.getId()).orElseThrow();
            Point unsavedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
            List<Order> orders = orderRepository.findAllByUserId(user.getId());

            assertAll(
                    () -> assertThat(unsavedStock1.getQuantity()).isEqualTo(1),
                    () -> assertThat(unsavedStock2.getQuantity()).isEqualTo(1),
                    () -> assertThat(unsavedPoint.getBalance()).isEqualTo(Money.of(0)),
                    () -> assertThat(orders.isEmpty()).isTrue()
            );
        }
    }

    @DisplayName("주문 조회할 때,")
    @Nested
    class getOrder {

        @DisplayName("로그인한 유저의 주문 목록이 정상 조회된다.")
        @Test
        void succeeds_whenRetrievingOrderListByUserId() {
            // arrange
            User user = userRepository.save(createValidUser());

            Order order1 = orderRepository.save(Order.create(user.getId()));
            Order order2 = orderRepository.save(Order.create(user.getId()));
            Order order3 = orderRepository.save(Order.create(1000L));

            // act
            List<Order> result = orderFacade.getOrders(user.getId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.size()).isEqualTo(2)
            );

        }

        @Transactional
        @DisplayName("로그인한 유저의 단일 주문이 정상 조회된다.")
        @Test
        void succeeds_whenRetrievingOrderByUserId() {
            // arrange
            User user = userRepository.save(createValidUser());

            Product product = Product.create(1L, "상품이름1", Money.of(1000)
);
            productRepository.save(product);

            Order order = orderRepository.save(Order.create(user.getId()));
            order.addItem(
                    product.getId(),
                    1,
                    product.getPrice(),
                    product.getPrice().multiply(1)
            );
            orderRepository.save(order);

            // act
            Order result = orderFacade.getOrder(user.getId(), order.getId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getItems().size()).isEqualTo(1),
                    () -> assertThat(result.getItems().get(0).getQuantity()).isEqualTo(1),
                    () -> assertThat(result.getItems().get(0).getUnitPrice()).isEqualTo(product.getPrice())

            );

        }

        @DisplayName("존재하지 않는 주문ID인 경우 조회 실패한다.")
        @Test
        void fails_whenOrderIdDoesNotExist() {
            // arrange
            Long invalidOrderId = 999L;

            User user = userRepository.save(createValidUser());

            Order order1 = orderRepository.save(Order.create(user.getId()));
            Order order2 = orderRepository.save(Order.create(user.getId()));
            Order order3 = orderRepository.save(Order.create(1000L));

            // act + assert
            assertThatThrownBy(() -> orderFacade.getOrder(user.getId(), invalidOrderId))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("존재하지 않는 주문입니다.")
                    .extracting("errorType")
                    .isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
