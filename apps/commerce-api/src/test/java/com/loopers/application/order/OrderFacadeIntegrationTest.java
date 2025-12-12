package com.loopers.application.order;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponPolicy;
import com.loopers.domain.coupon.CouponPolicyRepository;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponStatus;
import com.loopers.support.test.IntegrationTestSupport;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
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
import com.loopers.domain.order.event.OrderCreatedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DisplayName("OrderFacade 통합 테스트")
class OrderFacadeIntegrationTest extends IntegrationTestSupport {

  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  private static final int ASYNC_TIMEOUT_MS = 5000;

  @Autowired
  private OrderFacade orderFacade;
  @Autowired
  private ProductRepository productRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PointRepository pointRepository;
  @Autowired
  private CouponPolicyRepository couponPolicyRepository;
  @Autowired
  private CouponRepository couponRepository;
  @MockitoSpyBean
  private OrderEventHandler orderEventHandler;

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
    @DisplayName("포인트 전액 결제 시 주문이 생성되고 포인트 전액 결제 핸들러가 호출된다")
    void createOrder_pointOnlyPayment_success() {
      // given
      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(2L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands);

      // then - 주문 생성 검증 (동기)
      assertThat(order)
          .extracting("totalAmountValue", "status", "pgAmountValue")
          .containsExactly(50000L, OrderStatus.PENDING, 0L);

      // then - 비동기 이벤트 핸들러 호출 검증
      verify(orderEventHandler, timeout(ASYNC_TIMEOUT_MS))
          .handlePointDeduction(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("포인트 부족 시 PG 결제 금액이 계산되고 PENDING 상태가 되며 포인트 차감 핸들러가 호출된다")
    void createOrder_pgPaymentRequired_pendingStatus() {
      // given
      Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
      point.deduct(Money.of(80000L));
      pointRepository.save(point);

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(2L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands);

      // then - 주문 생성 검증 (동기)
      assertThat(order)
          .extracting("totalAmountValue", "status", "pointUsedAmountValue", "pgAmountValue")
          .containsExactly(50000L, OrderStatus.PENDING, 20000L, 30000L);

      // then - 비동기 이벤트 핸들러 호출 검증
      verify(orderEventHandler, timeout(ASYNC_TIMEOUT_MS))
          .handlePointDeduction(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("쿠폰 적용 시 할인 금액이 반영되고 쿠폰 사용 핸들러가 호출된다")
    void createOrder_withCoupon_appliesDiscount() {
      // given
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(10000L)));
      Coupon coupon = couponRepository.save(Coupon.of(user.getId(), policy));

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(2L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L))
      );

      // when
      Order order = orderFacade.createOrder(user.getId(), commands, coupon.getId());

      // then - 주문 생성 검증 (동기)
      assertThat(order)
          .extracting("totalAmountValue", "discountAmountValue", "pointUsedAmountValue", "pgAmountValue", "status")
          .containsExactly(50000L, 10000L, 40000L, 0L, OrderStatus.PENDING);

      // then - 비동기 이벤트 핸들러 호출 검증
      verify(orderEventHandler, timeout(ASYNC_TIMEOUT_MS))
          .handlePointDeduction(any(OrderCreatedEvent.class));
      verify(orderEventHandler, timeout(ASYNC_TIMEOUT_MS))
          .handleCouponUsage(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("타인 소유 쿠폰 적용 시 예외가 발생하고 트랜잭션이 롤백된다")
    void createOrder_withOtherUserCoupon_throwsException() {
      // given
      User anotherUser = userRepository.save(User.of("another", "another@example.com", BIRTH_DATE_1990_01_01, Gender.FEMALE, LocalDate.of(2025, 10, 30)));
      CouponPolicy policy = couponPolicyRepository.save(CouponPolicy.ofFixed(Money.of(10000L)));
      Coupon anothersCoupon = couponRepository.save(Coupon.of(anotherUser.getId(), policy));

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(2L))
      );

      // when & then
      assertThatThrownBy(() -> orderFacade.createOrder(user.getId(), commands, anothersCoupon.getId()))
          .isInstanceOf(CoreException.class)
          .extracting("errorType")
          .isEqualTo(ErrorType.COUPON_NOT_OWNED);

      Coupon unchangedCoupon = couponRepository.findById(anothersCoupon.getId()).orElseThrow();
      Point unchangedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();

      assertThat(unchangedCoupon.getStatus()).isEqualTo(CouponStatus.AVAILABLE);
      assertThat(unchangedPoint.getAmountValue()).isEqualTo(100000L);
    }
  }
}
