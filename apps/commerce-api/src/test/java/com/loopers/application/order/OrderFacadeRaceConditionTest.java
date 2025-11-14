package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.orderitem.OrderItemCommand;
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
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("OrderFacade 동시성 테스트")
class OrderFacadeRaceConditionTest {

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

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  private static final LocalDateTime ORDERED_AT_2025_10_30 = LocalDateTime.of(2025, 10, 30, 0, 0, 0);
  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Test
  @DisplayName("동시에 2개 주문 시 하나만 성공하고 재고가 음수가 되지 않음")
  void concurrentOrders_onlyOneSucceeds() throws InterruptedException {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고1개상품", Money.of(10000L), "재고 1개 상품", Stock.of(1L), brand.getId())
    );

    int random = (int) (System.nanoTime() % 10000);
    User user1 = userRepository.save(User.of("u1" + random, "u1" + random + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE));
    User user2 = userRepository.save(User.of("u2" + random, "u2" + random + "@t.com", BIRTH_DATE_1990_01_01, Gender.FEMALE));
    pointRepository.save(Point.of(user1.getId(), 50000L));
    pointRepository.save(Point.of(user2.getId(), 50000L));

    int threadCount = 2;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1))
    );

    try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
      executorService.submit(() -> {
        try {
          orderFacade.createOrder(user1.getId(), commands, ORDERED_AT_2025_10_30);
          successCount.incrementAndGet();
        } catch (CoreException e) {
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });

      executorService.submit(() -> {
        try {
          orderFacade.createOrder(user2.getId(), commands, ORDERED_AT_2025_10_30);
          successCount.incrementAndGet();
        } catch (CoreException e) {
          failureCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });

      latch.await();
    }

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(1);

    Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updatedProduct.getStockValue()).isZero();
  }

  @Test
  @DisplayName("동시에 10개 주문 시 재고 5개 상품은 5개만 성공")
  void concurrentOrders_multipleThreads() throws InterruptedException {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고5개상품", Money.of(5000L), "재고 5개 상품", Stock.of(5L), brand.getId())
    );

    int threadCount = 10;
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1))
    );

    AtomicInteger counter = new AtomicInteger(0);
    try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
          try {
            int id = counter.getAndIncrement();
            String loginId = "u" + id;
            User user = userRepository.save(User.of(loginId, loginId + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE));
            pointRepository.save(Point.of(user.getId(), 50000L));
            orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);
            successCount.incrementAndGet();
          } catch (CoreException e) {
            failureCount.incrementAndGet();
          } finally {
            latch.countDown();
          }
        });
      }

      latch.await();
    }

    assertThat(successCount.get()).isEqualTo(5);
    assertThat(failureCount.get()).isEqualTo(5);

    Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updatedProduct.getStockValue()).isZero();
  }

  @Test
  @DisplayName("동시 주문 시 재고가 음수가 되지 않음")
  void concurrentOrders_stockNeverNegative() throws InterruptedException {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고3개상품", Money.of(3000L), "재고 3개 상품", Stock.of(3L), brand.getId())
    );

    int threadCount = 5;
    CountDownLatch latch = new CountDownLatch(threadCount);

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1))
    );

    AtomicInteger counter = new AtomicInteger(100);
    try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
      for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
          try {
            int id = counter.getAndIncrement();
            String loginId = "u" + id;
            User user = userRepository.save(User.of(loginId, loginId + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE));
            pointRepository.save(Point.of(user.getId(), 50000L));
            orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);
          } catch (CoreException ignored) {
            // 재고 부족 시 예외 발생 예상
          } finally {
            latch.countDown();
          }
        });
      }

      latch.await();
    }

    Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(finalProduct.getStockValue()).isGreaterThanOrEqualTo(0L);
  }
}