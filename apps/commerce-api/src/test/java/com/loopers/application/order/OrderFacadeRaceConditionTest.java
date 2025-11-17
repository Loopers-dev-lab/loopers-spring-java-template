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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
  void concurrentOrders_onlyOneSucceeds() {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고1개상품", Money.of(10000L), "재고 1개 상품", Stock.of(1L), brand.getId())
    );

    int random = (int) (System.nanoTime() % 10000);
    User user1 = userRepository.save(User.of("u1" + random, "u1" + random + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 10, 30)));
    User user2 = userRepository.save(User.of("u2" + random, "u2" + random + "@t.com", BIRTH_DATE_1990_01_01, Gender.FEMALE, LocalDate.of(2025, 10, 30)));
    pointRepository.save(Point.of(user1.getId(), 50000L));
    pointRepository.save(Point.of(user2.getId(), 50000L));

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1L))
    );

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    futures.add(CompletableFuture.runAsync(() -> {
      try {
        orderFacade.createOrder(user1.getId(), commands, ORDERED_AT_2025_10_30);
      } catch (CoreException ignored) {
        // 재고 부족 시 예외 발생 예상
      }
    }));

    futures.add(CompletableFuture.runAsync(() -> {
      try {
        orderFacade.createOrder(user2.getId(), commands, ORDERED_AT_2025_10_30);
      } catch (CoreException ignored) {
        // 재고 부족 시 예외 발생 예상
      }
    }));

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updatedProduct).extracting("stockValue").isEqualTo(0L);
  }

  @Test
  @DisplayName("동시에 10개 주문 시 재고 5개 상품은 5개만 성공")
  void concurrentOrders_multipleThreads() {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고5개상품", Money.of(5000L), "재고 5개 상품", Stock.of(5L), brand.getId())
    );

    int threadCount = 10;

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1L))
    );

    AtomicInteger counter = new AtomicInteger(0);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(CompletableFuture.runAsync(() -> {
        try {
          int id = counter.getAndIncrement();
          String loginId = "u" + id;
          User user = userRepository.save(User.of(loginId, loginId + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 10, 30)));
          pointRepository.save(Point.of(user.getId(), 50000L));
          orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);
        } catch (CoreException ignored) {
          // 재고 부족 시 예외 발생 예상
        }
      }));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updatedProduct).extracting("stockValue").isEqualTo(0L);
  }

  @Test
  @DisplayName("동시 주문 시 재고가 음수가 되지 않음")
  void concurrentOrders_stockNeverNegative() {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product = productRepository.save(
        Product.of("재고3개상품", Money.of(3000L), "재고 3개 상품", Stock.of(3L), brand.getId())
    );

    int threadCount = 5;

    List<OrderItemCommand> commands = List.of(
        OrderItemCommand.of(product.getId(), Quantity.of(1L))
    );

    AtomicInteger counter = new AtomicInteger(100);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      futures.add(CompletableFuture.runAsync(() -> {
        try {
          int id = counter.getAndIncrement();
          String loginId = "u" + id;
          User user = userRepository.save(User.of(loginId, loginId + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 10, 30)));
          pointRepository.save(Point.of(user.getId(), 50000L));
          orderFacade.createOrder(user.getId(), commands, ORDERED_AT_2025_10_30);
        } catch (CoreException ignored) {
          // 재고 부족 시 예외 발생 예상
        }
      }));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    Product finalProduct = productRepository.findById(product.getId()).orElseThrow();
    assertThat(finalProduct).extracting("stockValue").matches(value -> (Long) value >= 0L);
  }

  @Test
  @DisplayName("여러 상품 역순 동시 주문 시 데드락이 발생하지 않음 (ORDER BY 검증)")
  void concurrentOrders_multipleProducts_noDeadlock() {
    Brand brand = brandRepository.save(Brand.of("테스트브랜드"));
    Product product1 = productRepository.save(
        Product.of("상품1", Money.of(1000L), "상품 1", Stock.of(10L), brand.getId())
    );
    Product product2 = productRepository.save(
        Product.of("상품2", Money.of(2000L), "상품 2", Stock.of(10L), brand.getId())
    );
    Product product3 = productRepository.save(
        Product.of("상품3", Money.of(3000L), "상품 3", Stock.of(10L), brand.getId())
    );

    int random1 = (int) (System.nanoTime() % 10000);
    int random2 = random1 + 1;
    User user1 = userRepository.save(User.of("u1" + random1, "u1" + random1 + "@t.com", BIRTH_DATE_1990_01_01, Gender.MALE, LocalDate.of(2025, 10, 30)));
    User user2 = userRepository.save(User.of("u2" + random2, "u2" + random2 + "@t.com", BIRTH_DATE_1990_01_01, Gender.FEMALE, LocalDate.of(2025, 10, 30)));
    pointRepository.save(Point.of(user1.getId(), 100000L));
    pointRepository.save(Point.of(user2.getId(), 100000L));

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

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    futures.add(CompletableFuture.runAsync(() -> {
      orderFacade.createOrder(user1.getId(), commands1, ORDERED_AT_2025_10_30);
    }));

    futures.add(CompletableFuture.runAsync(() -> {
      orderFacade.createOrder(user2.getId(), commands2, ORDERED_AT_2025_10_30);
    }));

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    List<Product> finalProducts = List.of(
        productRepository.findById(product1.getId()).orElseThrow(),
        productRepository.findById(product2.getId()).orElseThrow(),
        productRepository.findById(product3.getId()).orElseThrow()
    );

    assertThat(finalProducts)
        .extracting("stockValue")
        .containsExactly(8L, 8L, 8L);
  }
}