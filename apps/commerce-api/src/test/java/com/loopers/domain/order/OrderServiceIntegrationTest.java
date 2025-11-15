package com.loopers.domain.order;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.BirthDate;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.point.PointModel;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OrderServiceIntegrationTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {
        
        @DisplayName("정상 주문이 성공적으로 생성된다")
        @Test
        void createsOrder_whenValidOrderRequest() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            pointJpaRepository.save(
                new PointModel(user, new Money(50000))
            );
            ProductModel product1 = productJpaRepository.save(
                new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(10))
            );
            ProductModel product2 = productJpaRepository.save(
                new ProductModel("product2", new Brand("Samsung"), new Money(20000), new Quantity(5))
            );

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(product1.getId(), 2),
                new OrderService.OrderItemRequest(product2.getId(), 1)
            );

            // act
            OrderModel order = orderService.createOrder(user, items);

            // assert
            assertAll(
                () -> assertThat(order).isNotNull(),
                () -> assertThat(order.getUser()).isEqualTo(user),
                () -> assertThat(order.getTotalPrice().value()).isEqualTo(40000), // 10000*2 + 20000*1
                () -> assertThat(order.getOrderItems()).hasSize(2),
                () -> assertThat(order.getOrderItems().get(0).getProduct().getId()).isEqualTo(product1.getId()),
                () -> assertThat(order.getOrderItems().get(0).getQuantity().quantity()).isEqualTo(2),
                () -> assertThat(order.getOrderItems().get(1).getProduct().getId()).isEqualTo(product2.getId()),
                () -> assertThat(order.getOrderItems().get(1).getQuantity().quantity()).isEqualTo(1)
            );
        }

        @DisplayName("재고 부족 시 주문 생성이 실패한다")
        @Test
        void throwsException_whenInsufficientStock() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            pointJpaRepository.save(
                new PointModel(user, new Money(50000))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(5))
            );

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(product.getId(), 10) // 재고보다 많은 수량
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.createOrder(user, items);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("재고가 부족합니다");
        }

        @DisplayName("포인트 부족 시 주문 생성이 실패한다")
        @Test
        void throwsException_whenInsufficientPoints() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            pointJpaRepository.save(
                new PointModel(user, new Money(10000)) // 부족한 포인트
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product1", new Brand("Apple"), new Money(20000), new Quantity(10))
            );

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(product.getId(), 1) // 20000원 필요
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.createOrder(user, items);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("포인트가 부족합니다");
        }

        @DisplayName("존재하지 않는 상품으로 주문 시 실패한다")
        @Test
        void throwsException_whenProductNotFound() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            pointJpaRepository.save(
                new PointModel(user, new Money(50000))
            );

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(999L, 1) // 존재하지 않는 상품 ID
            );

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                orderService.createOrder(user, items);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다");
        }

        @DisplayName("주문 생성 시 재고가 정확히 차감된다")
        @Test
        void decreasesStock_whenOrderIsCreated() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            pointJpaRepository.save(
                new PointModel(user, new Money(50000))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(10))
            );
            int initialQuantity = product.getQuantity().quantity();

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(product.getId(), 3)
            );

            // act
            orderService.createOrder(user, items);

            // assert
            ProductModel updatedProduct = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updatedProduct.getQuantity().quantity()).isEqualTo(initialQuantity - 3);
        }

        @DisplayName("주문 생성 시 포인트가 정확히 차감된다")
        @Test
        void decreasesPoints_whenOrderIsCreated() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            PointModel point = pointJpaRepository.save(
                new PointModel(user, new Money(50000))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product1", new Brand("Apple"), new Money(10000), new Quantity(10))
            );
            long initialPoints = point.getPoint().value();

            List<OrderService.OrderItemRequest> items = List.of(
                new OrderService.OrderItemRequest(product.getId(), 2)
            );

            // act
            orderService.createOrder(user, items);

            // assert
            PointModel updatedPoint = pointJpaRepository.findByUser(user).orElseThrow();
            assertThat(updatedPoint.getPoint().value()).isEqualTo(initialPoints - 20000);
        }
    }
}

