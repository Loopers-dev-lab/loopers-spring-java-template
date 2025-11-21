package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.money.Money;
import com.loopers.domain.order.OrderCommand.Item;
import com.loopers.domain.order.OrderCommand.PlaceOrder;
import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderConcurrencyTest {

  @Autowired
  private OrderFacade orderFacade;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Autowired
  private ProductJpaRepository productJpaRepository;

  @AfterEach
  void tearDown() {
    userJpaRepository.deleteAll();
    productJpaRepository.deleteAll();
  }

  @Nested
  @DisplayName("포인트 동시성 (비관적 락)")
  class PointConcurrency {

    private User savedUser;
    private List<Product> distinctProducts;

    @BeforeEach
    void setUp() {
      User user = new User("testUser", "test@email.com", "1990-01-01", User.Gender.MALE,
          new Point(10000L));
      this.savedUser = userJpaRepository.saveAndFlush(user);

      this.distinctProducts = IntStream.range(0, 10)
          .mapToObj(i -> {
            Product product = new Product(1l, "상품" + i, "설명", new Money(1000L), 100);
            return productJpaRepository.saveAndFlush(product);
          })
          .toList();
    }

    @Test
    @DisplayName("동시에 10번 주문 시, 비관적 락으로 인해 포인트는 순차적으로 정확히 차감되어야 한다.")
    void point_deduction_concurrency_test() {
      // arrange
      int threadCount = 10;
      ExecutorService executorService = Executors.newFixedThreadPool(32);

      AtomicInteger successCount = new AtomicInteger();
      AtomicInteger failCount = new AtomicInteger();

      // act
      List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
          .mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
              Product targetProduct = distinctProducts.get(i);
              PlaceOrder command = new PlaceOrder(savedUser.getId(),
                  List.of(new Item(targetProduct.getId(), 1)));

              orderFacade.placeOrder(command);

              successCount.getAndIncrement();
            } catch (Exception e) {
              System.out.println("주문 실패: " + e.getMessage());
              failCount.getAndIncrement();
            }
          }, executorService))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // assert
      User findUser = userRepository.findById(savedUser.getId()).orElseThrow();
      long expectedPoint = 10000L - (1000L * threadCount);

      assertThat(successCount.get()).isEqualTo(threadCount);
      assertThat(findUser.getPoint().getAmount()).isEqualTo(expectedPoint);
    }
  }

  @Nested
  @DisplayName("재고 동시성 (낙관적 락)")
  class StockConcurrency {

    private Product savedProduct;

    @BeforeEach
    void setUp() {
      Product product = new Product(1L, "인기상품", "설명", new Money(1000L), 100);
      this.savedProduct = productJpaRepository.saveAndFlush(product);
    }

    @Test
    @DisplayName("서로 다른 10명이 동시에 주문 시, 낙관적 락 충돌이 발생해도 재고 정합성은 유지되어야 한다.")
    void stock_deduction_concurrency_test() {
      // arrange
      int threadCount = 10;

      List<User> multiUsers = IntStream.range(0, threadCount)
          .mapToObj(i -> {
            User user = new User("user" + i, "user" + i + "@test.com", "2000-01-01",
                User.Gender.FEMALE, new Point(10000L));
            return userJpaRepository.saveAndFlush(user);
          })
          .toList();

      ExecutorService executorService = Executors.newFixedThreadPool(32);

      AtomicInteger successCount = new AtomicInteger();
      AtomicInteger failCount = new AtomicInteger();

      // act
      List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
          .mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
              PlaceOrder command = new PlaceOrder(multiUsers.get(i).getId(),
                  List.of(new Item(savedProduct.getId(), 1)));

              orderFacade.placeOrder(command);

              successCount.getAndIncrement();
            } catch (Exception e) {
              System.out.println("주문 실패(충돌 감지): " + e.getMessage());
              failCount.getAndIncrement();
            }
          }, executorService))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      // assert
      Product findProduct = productJpaRepository.findById(savedProduct.getId()).orElseThrow();
      long expectedStock = savedProduct.getStock() - successCount.get();

      System.out.println("성공: " + successCount.get() + ", 충돌 실패: " + failCount.get());
      assertThat(findProduct.getStock()).isEqualTo(expectedStock);
    }
  }
}
