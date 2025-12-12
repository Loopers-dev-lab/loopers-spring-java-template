package com.loopers.application.order;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.quantity.Quantity;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.test.IntegrationTestSupport;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("OrderFacade 동시성 테스트")
class OrderFacadeRaceConditionTest extends IntegrationTestSupport {

  private static final Logger log = LoggerFactory.getLogger(OrderFacadeRaceConditionTest.class);

  @Autowired
  private OrderFacade orderFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PointRepository pointRepository;

  @Autowired
  private OrderJpaRepository orderJpaRepository;

  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  private static final LocalDate JOINED_AT_2025_10_30 = LocalDate.of(2025, 10, 30);
  private static final int ASYNC_EVENT_TIMEOUT_SECONDS = 10;

  private static final long POINT_BALANCE_50_000 = 50000L;
  private static final long BRAND_ID = 1L;

  @DisplayName("재고 동시성 테스트")
  @Nested
  class StockConcurrency {

    @Test
    @DisplayName("재고 1개 상품에 2명이 동시 주문 시 1개만 성공하고 재고가 0이 된다")
    void concurrentOrders_onlyOneSucceeds() {
      // given
      Product product = productRepository.save(
          Product.of("재고1개상품", Money.of(10000L), "재고 1개 상품", Stock.of(1L), BRAND_ID)
      );

      User user1 = createUserWithPoint("user1", "user1@test.com", Gender.MALE, POINT_BALANCE_50_000);
      User user2 = createUserWithPoint("user2", "user2@test.com", Gender.FEMALE, POINT_BALANCE_50_000);

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product.getId(), Quantity.of(1L))
      );

      // when
      List<Long> orderIds = new CopyOnWriteArrayList<>();
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      futures.add(asyncExecuteWithOrderId(() -> orderFacade.createOrder(user1.getId(), commands), orderIds));
      futures.add(asyncExecuteWithOrderId(() -> orderFacade.createOrder(user2.getId(), commands), orderIds));

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS).untilAsserted(() -> {
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        long completedCount = countCompletedOrders(orderIds);
        assertThat(updatedProduct.getStockValue()).isZero();
        assertThat(completedCount).isEqualTo(1);
      });
    }

    @Test
    @DisplayName("재고 5개 상품에 10명이 동시 주문 시 5개만 성공하고 재고가 0이 된다")
    void concurrentOrders_multipleThreads() {
      // given
      Product product = productRepository.save(
          Product.of("재고5개상품", Money.of(5000L), "재고 5개 상품", Stock.of(5L), BRAND_ID)
      );

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product.getId(), Quantity.of(1L))
      );

      // when
      AtomicInteger userIdCounter = new AtomicInteger(10);
      List<Long> orderIds = new CopyOnWriteArrayList<>();
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (int i = 0; i < 10; i++) {
        futures.add(asyncExecuteWithOrderId(() -> {
          int userId = userIdCounter.getAndIncrement();
          String loginId = "user" + userId;
          User user = createUserWithPoint(loginId, loginId + "@test.com", Gender.MALE, POINT_BALANCE_50_000);
          return orderFacade.createOrder(user.getId(), commands);
        }, orderIds));
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS).untilAsserted(() -> {
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        long completedCount = countCompletedOrders(orderIds);
        assertThat(updatedProduct.getStockValue()).isZero();
        assertThat(completedCount).isEqualTo(5);
      });
    }

    @Test
    @DisplayName("재고 3개 상품에 5명이 동시 주문 시 재고가 음수가 되지 않고 최대 3명만 성공한다")
    void concurrentOrders_stockNeverNegative() {
      // given
      long initialStock = 3L;
      Product product = productRepository.save(
          Product.of("재고3개상품", Money.of(3000L), "재고 3개 상품", Stock.of(initialStock), BRAND_ID)
      );

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product.getId(), Quantity.of(1L))
      );

      // when
      AtomicInteger userIdCounter = new AtomicInteger(20);
      List<Long> orderIds = new CopyOnWriteArrayList<>();
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (int i = 0; i < 5; i++) {
        futures.add(asyncExecuteWithOrderId(() -> {
          int userId = userIdCounter.getAndIncrement();
          String loginId = "user" + userId;
          User user = createUserWithPoint(loginId, loginId + "@test.com", Gender.MALE, POINT_BALANCE_50_000);
          return orderFacade.createOrder(user.getId(), commands);
        }, orderIds));
      }

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS).untilAsserted(() -> {
        Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
        long completedCount = countCompletedOrders(orderIds);
        assertThat(finalProduct.getStockValue()).isGreaterThanOrEqualTo(0L);
        assertThat(completedCount).isLessThanOrEqualTo(initialStock);
      });
    }
  }

  @DisplayName("포인트 동시성 테스트")
  @Nested
  class PointConcurrency {

    @Test
    @DisplayName("동일한 유저가 서로 다른 상품 2개를 동시 주문 시 포인트가 정확히 차감된다")
    void concurrentOrders_sameUser_differentProducts() {
      // given
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(10000L), "상품 1", Stock.of(10L), BRAND_ID)
      );
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(20000L), "상품 2", Stock.of(10L), BRAND_ID)
      );

      User user = createUserWithPoint("user30", "user30@test.com", Gender.MALE, POINT_BALANCE_50_000);

      List<OrderItemCommand> commands1 = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(1L))
      );
      List<OrderItemCommand> commands2 = List.of(
          OrderItemCommand.of(product2.getId(), Quantity.of(1L))
      );

      // when
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      futures.add(asyncExecute(() -> orderFacade.createOrder(user.getId(), commands1)));
      futures.add(asyncExecute(() -> orderFacade.createOrder(user.getId(), commands2)));

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS).untilAsserted(() -> {
        Point finalPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(finalPoint.getAmountValue()).isEqualTo(20000L);
      });
    }
  }

  @DisplayName("데드락 방지 테스트")
  @Nested
  class DeadlockPrevention {

    @Test
    @DisplayName("여러 상품 역순 동시 주문 시 데드락이 발생하지 않는다 (ORDER BY 검증)")
    void concurrentOrders_multipleProducts_noDeadlock() {
      // given
      Product product1 = productRepository.save(
          Product.of("상품1", Money.of(1000L), "상품 1", Stock.of(10L), BRAND_ID)
      );
      Product product2 = productRepository.save(
          Product.of("상품2", Money.of(2000L), "상품 2", Stock.of(10L), BRAND_ID)
      );
      Product product3 = productRepository.save(
          Product.of("상품3", Money.of(3000L), "상품 3", Stock.of(10L), BRAND_ID)
      );

      User user1 = createUserWithPoint("user40", "user40@test.com", Gender.MALE, POINT_BALANCE_50_000);
      User user2 = createUserWithPoint("user41", "user41@test.com", Gender.FEMALE, POINT_BALANCE_50_000);

      List<OrderItemCommand> commands1 = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(1L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L)),
          OrderItemCommand.of(product3.getId(), Quantity.of(1L))
      );

      List<OrderItemCommand> commands2 = List.of(
          OrderItemCommand.of(product3.getId(), Quantity.of(1L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(1L)),
          OrderItemCommand.of(product1.getId(), Quantity.of(1L))
      );

      // when
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      futures.add(asyncExecute(() -> orderFacade.createOrder(user1.getId(), commands1)));
      futures.add(asyncExecute(() -> orderFacade.createOrder(user2.getId(), commands2)));

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(10, TimeUnit.SECONDS)
          .join();

      // then - 비동기 이벤트 핸들러 완료 대기
      await().atMost(ASYNC_EVENT_TIMEOUT_SECONDS, SECONDS).untilAsserted(() -> {
        List<Product> finalProducts = List.of(
            productRepository.findById(product1.getId()).orElseThrow(),
            productRepository.findById(product2.getId()).orElseThrow(),
            productRepository.findById(product3.getId()).orElseThrow()
        );

        assertThat(finalProducts)
            .extracting("stockValue")
            .containsExactly(8L, 8L, 8L);
      });
    }
  }

  private CompletableFuture<Void> asyncExecute(Runnable task) {
    return CompletableFuture.runAsync(() -> {
      try {
        task.run();
      } catch (Exception e) {
        log.debug("동시성 테스트 중 예외 발생 (예상됨): {}", e.getMessage());
      }
    });
  }

  private CompletableFuture<Void> asyncExecuteWithCount(Runnable task, AtomicInteger successCount) {
    return CompletableFuture.runAsync(() -> {
      try {
        task.run();
        successCount.incrementAndGet();
      } catch (Exception e) {
        log.debug("동시성 테스트 중 예외 발생 (예상됨): {}", e.getMessage());
      }
    });
  }

  private CompletableFuture<Void> asyncExecuteWithOrderId(Supplier<Order> task, List<Long> orderIds) {
    return CompletableFuture.runAsync(() -> {
      try {
        Order order = task.get();
        orderIds.add(order.getId());
      } catch (Exception e) {
        log.debug("동시성 테스트 중 예외 발생 (예상됨): {}", e.getMessage());
      }
    });
  }

  private long countCompletedOrders(List<Long> orderIds) {
    List<Long> distinctIds = orderIds.stream().distinct().toList();
    if (distinctIds.isEmpty()) {
      log.warn("완료된 주문 조회 시 orderIds가 비어있습니다.");
      return 0;
    }
    return orderJpaRepository.findAllById(distinctIds).stream()
        .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
        .count();
  }

  private User createUserWithPoint(String loginId, String email, Gender gender, Long pointBalance) {
    User user = userRepository.save(
        User.of(loginId, email, BIRTH_DATE_1990_01_01, gender, JOINED_AT_2025_10_30)
    );
    pointRepository.save(Point.of(user.getId(), pointBalance));
    return user;
  }
}
