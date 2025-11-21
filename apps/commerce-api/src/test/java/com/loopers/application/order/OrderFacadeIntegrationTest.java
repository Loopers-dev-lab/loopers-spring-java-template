package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.money.Money;
import com.loopers.domain.order.OrderCommand;
import com.loopers.domain.order.OrderCommand.Item;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.User.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
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
  OrderFacade orderFacade;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("상품 주문시")
  @Nested
  class PlaceOrder {
    @DisplayName("주문이 성공하면 재고와 포인트가 차감되고 주문 정보가 반환된다.")
    @Test
    void placeOrder_success() {
      // arrange
      User user = new User("userA", "a@email.com", "2025-11-11", Gender.MALE, new Point(100000L));
      User savedUser = userRepository.save(user);

      Long brandId = 1L;
      Product product = new Product(brandId, "Product A", "설명", new Money(20000L), 10);
      Product saveProduct = productRepository.save(product);

      Item itemCommand = new Item(saveProduct.getId(), 3);
      OrderCommand.PlaceOrder command = new OrderCommand.PlaceOrder(savedUser.getId(), List.of(itemCommand));

      // act
      OrderInfo result = orderFacade.placeOrder(command);

      // assert
      assertAll(
          () -> assertThat(result).isNotNull(),
          () -> assertThat(result.totalAmount()).isEqualTo(60000L),
          () -> assertThat(result.items()).hasSize(1)
      );


      Product updatedProduct = productRepository.findById(saveProduct.getId()).orElseThrow();
      assertThat(updatedProduct.getStock()).isEqualTo(7);

      User updatedUser = userRepository.findByUserId("userA").orElseThrow();
      assertThat(updatedUser.getPoint().getAmount()).isEqualTo(40000L);
    }

    @DisplayName("재고가 부족하면 주문에 실패한다.")
    @Test
    void placeOrder_fail_insufficient_stock() {
      // arrange
      User user = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE, new Point(100000L)));

      Product product = productRepository.save(
          new Product(1L, "Product A", "설명", new Money(20000L), 2)
      );

      Item itemCommand = new Item(product.getId(), 3);
      OrderCommand.PlaceOrder command = new OrderCommand.PlaceOrder(user.getId(), List.of(itemCommand));

      // act & assert
      CoreException exception = assertThrows(CoreException.class, () -> {
        orderFacade.placeOrder(command);
      });

      assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("포인트가 부족하면 주문에 실패한다.")
    @Test
    void placeOrder_fail_insufficient_point() {
      // arrange
      User user = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE, new Point(10000L)));

      Product product = productRepository.save(
          new Product(1L, "Product A", "설명", new Money(20000L), 10)
      );

      Item itemCommand = new Item(product.getId(), 1);
      OrderCommand.PlaceOrder command = new OrderCommand.PlaceOrder(user.getId(), List.of(itemCommand));

      // act & assert
      CoreException exception = assertThrows(CoreException.class, () -> {
        orderFacade.placeOrder(command);
      });

      assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("포인트 잔액 부족으로 주문이 실패하면, 차감되었던 재고는 롤백되어 원상복구된다.")
    @Test
    void placeOrder_transaction_rollback_test() {
      // arrange
      Product product = productRepository.save(
          new Product(1L, "Product A", "설명", new Money(20000L), 10)
      );

      User user = userRepository.save(
          new User("userRollback", "rollback@email.com", "2025-11-11", Gender.MALE, new Point(0L))
      );

      Item itemCommand = new Item(product.getId(), 1); // 1개 주문 시도
      OrderCommand.PlaceOrder command = new OrderCommand.PlaceOrder(user.getId(), List.of(itemCommand));

      // act
      assertThrows(CoreException.class, () -> {
        orderFacade.placeOrder(command);
      });

      // assert
      Product rollbackedProduct = productRepository.findById(product.getId()).orElseThrow();

      assertThat(rollbackedProduct.getStock()).isEqualTo(10);
    }
  }


}
