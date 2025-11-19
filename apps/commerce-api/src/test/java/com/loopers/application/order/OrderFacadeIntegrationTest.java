package com.loopers.application.order;

import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.quantity.Quantity;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("OrderFacade 통합 테스트")
class OrderFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  @Autowired
  private OrderFacade orderFacade;
  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PointRepository pointRepository;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  private User user;
  private Product product1;
  private Product product2;

  @BeforeEach
  void setUp() {
    user = userRepository.save(User.of("testuser", "test@example.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 10, 30)));
    product1 = productRepository.save(
        Product.of("상품1", Money.of(10000L), "설명1", Stock.of(10L), 1L)
    );
    product2 = productRepository.save(
        Product.of("상품2", Money.of(30000L), "설명2", Stock.of(5L), 1L)
    );
    pointRepository.save(Point.of(user.getId(), 100000L));
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("주문 생성")
  class CreateOrder {

    @Test
    @DisplayName("재고가 0인 상품을 주문하면 예외가 발생한다")
    void createOrder_zeroStock() {
      // given
      Product zeroStockProduct = productRepository.save(
          Product.of("재고0상품", Money.of(10000L), "설명", Stock.of(0L), 1L)
      );

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(zeroStockProduct.getId(), Quantity.of(1L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("포인트가 부족하면 예외가 발생한다")
    void createOrder_insufficientPoint() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(1L))
      );

      Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
      point.deduct(95000L);
      pointRepository.save(point);

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_POINT_BALANCE);
    }


    @Test
    @DisplayName("재고가 부족하면 예외가 발생한다")
    void createOrder_insufficientStock() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(100L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_STOCK)
          .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("재고가 부족하면 트랜잭션이 롤백된다")
    void createOrder_rollbackWhenStockInsufficient() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(100L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_STOCK);

      Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

      assertThat(unchangedProduct.getStockValue()).isEqualTo(10L);
      assertThat(unchangedPoint.getAmountValue()).isEqualTo(100000L);
    }

    @Test
    @DisplayName("포인트가 부족하면 트랜잭션이 롤백된다")
    void createOrder_rollbackWhenPointInsufficient() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(1L))
      );

      Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
      point.deduct(95000L);
      pointRepository.save(point);

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_POINT_BALANCE);

      Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

      assertThat(unchangedProduct.getStockValue()).isEqualTo(10L);
      assertThat(unchangedPoint.getAmountValue()).isEqualTo(5000L);
    }


    @Test
    @DisplayName("유효한 주문 요청이면 주문이 생성되고 재고와 포인트가 차감된다")
    void createOrder_success() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(2L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands);

      // then
      Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
      Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
      Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

      assertAll(
          () -> assertThat(order)
              .extracting("totalAmountValue", "status")
              .containsExactly(50000L, OrderStatus.PENDING),
          () -> assertThat(updatedProduct1.getStockValue()).isEqualTo(8L),
          () -> assertThat(updatedProduct2.getStockValue()).isEqualTo(4L),
          () -> assertThat(updatedPoint.getAmountValue()).isEqualTo(50000L)
      );


    }

  }
}
