package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.orderitem.OrderPrice;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@DisplayName("OrderFacade 통합 테스트")
class OrderFacadeIntegrationTest {

  @Autowired
  private OrderFacade orderFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private BrandRepository brandRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PointRepository pointRepository;

  private User user;
  private Brand brand;
  private Product product1;
  private Product product2;

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);
  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);

  @BeforeEach
  void setUp() {
    user = userRepository.save(User.of("testuser", "test@example.com", BIRTH_DATE_1990_01_01, Gender.MALE));
    brand = brandRepository.save(Brand.of("테스트브랜드"));
    product1 = productRepository.save(
        Product.of("상품1", Money.of(10000L), "설명1", Stock.of(10L), brand.getId())
    );
    product2 = productRepository.save(
        Product.of("상품2", Money.of(30000L), "설명2", Stock.of(5L), brand.getId())
    );
    pointRepository.save(Point.of(user.getId(), 100000L));
  }

  @Nested
  @DisplayName("주문 생성")
  class CreateOrder {

    @Test
    @DisplayName("정상 주문 생성 - 재고 차감, 포인트 차감 확인")
    void createOrder_success() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), "상품1", Quantity.of(2), OrderPrice.of(10000L)),
          OrderItemCommand.of(product2.getId(), "상품2", Quantity.of(1), OrderPrice.of(30000L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);

      // then
      assertThat(order.getTotalAmountValue()).isEqualTo(50000L);
      assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

      // 재고 차감 확인 (실제 DB 조회)
      Product updatedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
      assertThat(updatedProduct1.getStockValue()).isEqualTo(8L);

      Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
      assertThat(updatedProduct2.getStockValue()).isEqualTo(4L);

      // 포인트 차감 확인
      Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
      assertThat(updatedPoint.getAmountValue()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생 - 롤백 확인")
    void createOrder_insufficientStock() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), "상품1", Quantity.of(100), OrderPrice.of(10000L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_STOCK)
          .hasMessageContaining("재고가 부족합니다");

      // 롤백 확인 - 재고 변경 안 됨
      Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
      assertThat(unchangedProduct.getStockValue()).isEqualTo(10L);

      // 포인트도 변경 안 됨
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
      assertThat(unchangedPoint.getAmountValue()).isEqualTo(100000L);
    }

    @Test
    @DisplayName("재고 0인 상품 - 예외 발생")
    void createOrder_zeroStock() {
      // given
      Product zeroStockProduct = productRepository.save(
          Product.of("재고0상품", Money.of(10000L), "설명", Stock.of(0L), brand.getId())
      );

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(zeroStockProduct.getId(), "재고0상품", Quantity.of(1), OrderPrice.of(10000L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("포인트 부족 시 예외 발생 - 롤백 확인")
    void createOrder_insufficientPoint() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), "상품1", Quantity.of(1), OrderPrice.of(10000L))
      );

      // 포인트를 5000으로 설정 (부족)
      Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
      point.deduct(95000L);

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30))
          .isInstanceOf(CoreException.class)
          .hasFieldOrPropertyWithValue("errorType", ErrorType.INSUFFICIENT_POINT_BALANCE);

      // 롤백 확인 - 재고 변경 안 됨
      Product unchangedProduct = productRepository.findById(product1.getId()).orElseThrow();
      assertThat(unchangedProduct.getStockValue()).isEqualTo(10L);

      // 포인트는 차감 전 상태 유지
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
      assertThat(unchangedPoint.getAmountValue()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("총액 계산 정확성 검증")
    void createOrder_totalAmountCalculation() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), "상품1", Quantity.of(3), OrderPrice.of(5000L)),
          OrderItemCommand.of(product2.getId(), "상품2", Quantity.of(2), OrderPrice.of(12500L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);

      // then
      assertThat(order.getTotalAmountValue()).isEqualTo(40000L);

      // 포인트 차감 확인
      Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
      assertThat(updatedPoint.getAmountValue()).isEqualTo(60000L);
    }
  }
}
