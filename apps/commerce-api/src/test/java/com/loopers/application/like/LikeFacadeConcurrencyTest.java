package com.loopers.application.like;

import com.loopers.application.order.CreateOrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockFixture;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderCreateV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class LikeFacadeConcurrencyTest {
  @Autowired
  private LikeFacade sut;
  @MockitoSpyBean
  private UserRepository userRepository;
  @MockitoSpyBean
  private UserService userService;
  @MockitoSpyBean
  private PointService pointService;
  @MockitoSpyBean
  private BrandService brandService;
  @MockitoSpyBean
  private ProductService productService;
  @MockitoSpyBean
  private LikeService likeService;

  @MockitoSpyBean
  private OrderService orderService;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;
  List<User> savedUsers;
  List<Brand> savedBrands;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // arrange
    List<User> userList = List.of(UserFixture.createUserWithLoginId("user1"), UserFixture.createUserWithLoginId("user2"));
    savedUsers = List.of(userService.join(userList.get(0)), userService.join(userList.get(1)));

    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    savedBrands = brandService.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProductWith("product1", Money.wons(1))
        , ProductFixture.createProductWith("product2", Money.wons(4))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productService.saveAll(productList);

  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("동시에 좋아요해도 정상적으로 처리된다.")
  @Test
  void 성공_좋아요_쓰레드10() throws InterruptedException {
    Long userId = savedUsers.get(0).getId();
    Long productId = savedProducts.get(0).getId();

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          sut.like(userId, productId);
        } catch (Exception e) {
          System.out.println("실패: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();

    Long likeCount = likeService.getLikeCount(productId);
    assertThat(likeCount).isEqualTo(1);
  }

  @DisplayName("동시에 좋아요 취소해도 정상적으로 처리된다.")
  @Test
  void 성공_좋아요_취소_쓰레드10() throws InterruptedException {
    Long userId = savedUsers.get(0).getId();
    Long productId = savedProducts.get(0).getId();

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger errorCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          sut.unlike(userId, productId);
        } catch (Exception e) {
          errorCount.getAndIncrement();
          System.out.println("실패: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    assertThat(errorCount.get()).isZero();
    Long likeCount = likeService.getLikeCount(productId);
    assertThat(likeCount).isZero();
  }

  @DisplayName("동일한 상품에 대해 여러명이 좋아요/싫어요를 요청해도, 상품의 좋아요 개수가 정상 반영되어야 한다.")
  @Test
  void 성공_동일한_상품_여러명_쓰레드10() throws InterruptedException {
    Long productId = savedProducts.get(0).getId();

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger errorCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      final int idx = i;
      executor.submit(() -> {
        try {
          Long userId = savedUsers.get(idx % savedUsers.size()).getId();
          if (idx % 2 == 0) {
            sut.like(userId, productId);
          } else {
            sut.unlike(userId, productId);
          }
        } catch (Exception e) {
          errorCount.incrementAndGet();
          System.out.println("스레드 실패 (idx=" + idx + "): " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();
    assertThat(errorCount.get()).isZero();
    Long likeCount = likeService.getLikeCount(productId);
    assertThat(likeCount).isBetween(0L, (long) savedUsers.size());
  }
}
