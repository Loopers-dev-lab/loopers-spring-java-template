package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;

import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
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
public class LikeConcurrencyTest {

  @Autowired
  private LikeFacade likeFacade;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Autowired
  private com.loopers.utils.DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("좋아요 증가 동시성 (낙관적  락)")
  class LikeIncreaseConcurrency {

    private Product targetProduct;
    private List<User> users;

    @BeforeEach
    void setUp() {
      Product product = new Product(1l, "인기상품", "설명", new Money(10000L), 100);
      this.targetProduct = productRepository.save(product);

      this.users = IntStream.range(0, 10)
          .mapToObj(i -> userJpaRepository.save(
              new User("user" + i, "user" + i + "@email.com", "2000-01-01", User.Gender.MALE)
          ))
          .toList();
    }

    @Test
    @DisplayName("동시에 10명이 좋아요를 눌러도, 최종 좋아요 개수와 성공 요청 수는 일치해야 한다.")
    void like_concurrency_test() {
      // arrange
      int threadCount = 10;
      ExecutorService executorService = Executors.newFixedThreadPool(32);

      AtomicInteger successCount = new AtomicInteger();
      AtomicInteger failCount = new AtomicInteger();

      // act
      List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
          .mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
              likeFacade.like(users.get(i).getId(), targetProduct.getId());
              successCount.getAndIncrement();
            } catch (Exception e) {
              System.out.println("좋아요 실패: " + e.getMessage());
              failCount.getAndIncrement();
            }
          }, executorService))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      Product resultProduct = productRepository.findById(targetProduct.getId()).orElseThrow();

      // assert
      assertThat(resultProduct.getLikeCount()).isEqualTo(successCount.get());
      assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
  }

  @Nested
  @DisplayName("좋아요 취소 동시성 (낙관적 락)")
  class LikeDecreaseConcurrency {

    private Product targetProduct;
    private List<User> users;

    @BeforeEach
    void setUp() {
      Product product = new Product(1l, "인기상품", "설명", new Money(10000L), 100);
      this.targetProduct = productRepository.save(product);

      this.users = IntStream.range(0, 10)
          .mapToObj(i -> userJpaRepository.save(
              new User("user" + i, "user" + i + "@email.com", "2000-01-01", User.Gender.MALE)
          ))
          .toList();

      users.forEach(user -> likeFacade.like(user.getId(), targetProduct.getId()));

      Product initial = productRepository.findById(targetProduct.getId()).orElseThrow();
      assertThat(initial.getLikeCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("이미 좋아요를 누른 10명이 동시에 취소를 요청하면, 좋아요 개수는 0개가 되어야 한다.")
    void unlike_concurrency_test() {
      // arrange
      int threadCount = 10;

      Product initialProduct = productRepository.findById(targetProduct.getId()).orElseThrow();
      assertThat(initialProduct.getLikeCount()).isEqualTo(10);

      ExecutorService executorService = Executors.newFixedThreadPool(32);

      AtomicInteger successCount = new AtomicInteger();
      AtomicInteger failCount = new AtomicInteger();

      // act
      List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
          .mapToObj(i -> CompletableFuture.runAsync(() -> {
            try {
              likeFacade.unLike(users.get(i).getId(), targetProduct.getId());
              successCount.getAndIncrement();
            } catch (Exception e) {
              System.out.println("취소 실패: " + e.getMessage());
              failCount.getAndIncrement();
            }
          }, executorService))
          .toList();

      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

      Product resultProduct = productRepository.findById(targetProduct.getId()).orElseThrow();

      // assert
      assertThat(resultProduct.getLikeCount()).isZero();
      assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
  }
}
