package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private BrandRepository brandRepository;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user1, user2;
    private Product product1;
    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();

        user1 = userService.signUp("user01", "order1@mail.com", "1990-01-01", Gender.MALE);
        user2 = userService.signUp("user02", "order2@mail.com", "1990-01-01", Gender.FEMALE);

        Brand brand = brandRepository.save(Brand.create("Order Brand"));
        product1 = productRepository.save(Product.create("Order Product 1", 1000L, 10, brand));

        order1 = Order.create(user1);
        order1.addOrderItem(product1, 1);
        orderService.save(order1);

        order2 = Order.create(user2);
        order2.addOrderItem(product1, 2);
        orderService.save(order2);
    }

    @DisplayName("주문 저장")
    @Nested
    class SaveOrder {

        @DisplayName("주문 저장 시, 주문 항목(OrderItem)도 Cascade로 함께 저장된다.")
        @Test
        @Transactional
        void saveOrder_and_CascadeItems() {
            // arrange
            Order newOrder = Order.create(user1);
            newOrder.addOrderItem(product1, 5);
            newOrder.completePayment();

            // act
            Order savedOrder = orderService.save(newOrder);

            // assert
            verify(orderRepository, times(3)).save(any(Order.class));
            Order foundOrder = orderRepository.findByIdAndUser(savedOrder.getId(), user1)
                    .orElseThrow(() -> new AssertionError("Order should be found"));

            assertAll(
                    () -> assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.PAID),
                    () -> assertThat(foundOrder.getTotalAmountValue()).isEqualTo(5000L),
                    () -> assertThat(foundOrder.getOrderItems()).hasSize(1)
            );
        }
    }

    @DisplayName("주문 조회")
    @Nested
    class GetOrder {

        @DisplayName("특정 사용자의 모든 주문 목록을 조회할 수 있다.")
        @Test
        void getOrdersByUser() {
            // act
            List<Order> user1Orders = orderService.getOrdersByUser(user1);
            List<Order> user2Orders = orderService.getOrdersByUser(user2);

            // assert
            assertThat(user1Orders).hasSize(1);
            assertThat(user1Orders.get(0).getId()).isEqualTo(order1.getId());

            assertThat(user2Orders).hasSize(1);
            assertThat(user2Orders.get(0).getId()).isEqualTo(order2.getId());
        }

        @DisplayName("특정 주문 ID와 사용자로 본인의 주문을 조회할 수 있다.")
        @Test
        void getOrderByIdAndUser_success() {
            // act
            Order foundOrder = orderService.getOrderByIdAndUser(order1.getId(), user1);

            // assert
            assertAll(
                    () -> assertThat(foundOrder).isNotNull(),
                    () -> assertThat(foundOrder.getId()).isEqualTo(order1.getId())
            );
        }

        @DisplayName("존재하지 않는 주문 ID로 조회 시, NOT_FOUND 예외가 발생한다.")
        @Test
        void getOrderByIdAndUser_throwsNotFound_whenOrderNotExist() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.getOrderByIdAndUser(-99L, user1);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("다른 사용자의 주문 ID로 조회 시, NOT_FOUND 예외가 발생한다.")
        @Test
        void getOrderByIdAndUser_throwsNotFound_whenUserIsDifferent() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.getOrderByIdAndUser(order1.getId(), user2);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
}
