package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class OrderFacadeConcurrencyTest {
  @Autowired
  private OrderFacade sut;
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
  private StockService stockService;

  @MockitoSpyBean
  private OrderService orderService;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;
  List<User> savedUsers;
  List<Brand> savedBrands;
  List<Product> savedProducts;

  @BeforeEach
  void setup() {
    // 사용자 생성
    savedUsers = List.of(
        userService.join(UserFixture.createUserWithLoginId("user1")),
        userService.join(UserFixture.createUserWithLoginId("user2"))
    );
    pointService.save(Point.create(savedUsers.get(0), BigDecimal.TEN));
    pointService.save(Point.create(savedUsers.get(1), BigDecimal.TEN));
    // 브랜드 생성
    savedBrands = brandService.saveAll(List.of(
        BrandFixture.createBrand(),
        BrandFixture.createBrand()
    ));

    // 상품 생성
    savedProducts = productService.saveAll(List.of(
        ProductFixture.createProductWith("product1", Money.wons(1)),
        ProductFixture.createProductWith("product2", Money.wons(4)),
        ProductFixture.createProduct(savedBrands.get(1))
    ));

    // 재고 생성
    stockService.save(StockFixture.createStockWith(savedProducts.get(0).getId(), 10));
    stockService.save(StockFixture.createStockWith(savedProducts.get(1).getId(), 10));
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감된다.")
  @Test
  void 재고_동시성_테스트() throws InterruptedException {
    Long productId = savedProducts.get(0).getId();
    CreateOrderCommand orderCommand = createOrderCommand(savedUsers.get(0).getId(), productId, 1);


    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          sut.createOrder(orderCommand);
        } catch (Exception e) {
          System.out.println("실패: " + e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();

    Stock stock = stockService.getStock(productId);
    assertThat(stock.getAvailable()).isZero();
  }

  @DisplayName("동일한 유저가 여러 주문을 동시에 수행해도, 포인트가 정상적으로 차감된다.")
  @Test
  void 포인트_동시성_테스트() throws InterruptedException {
    Long userId = savedUsers.get(0).getId();
    Long productId = savedProducts.get(1).getId();
    CreateOrderCommand orderCommand = createOrderCommand(userId, productId, 1);

    int threadCount = 10;
    AtomicInteger errorCount = new AtomicInteger();
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executor.submit(() -> {
        try {
          sut.createOrder(orderCommand);
        } catch (Exception e) {
          errorCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();

    // 재고 10개, 단가 4원 → 2개의 주문 성공, 나머지는 실패
    assertThat(errorCount.get()).isEqualTo(8);

    Stock stock = stockService.getStock(productId);
    assertThat(stock.getAvailable()).isEqualTo(8);

    BigDecimal remainingPoint = pointService.getAmount(userId);
    assertThat(remainingPoint).isEqualByComparingTo(new BigDecimal(2));
  }

  @DisplayName("동일한 유저가 서로 다른 주문을 동시에 수행해도, 포인트가 정상적으로 차감된다.")
  @Test
  void 동일유저_다른상품_동시주문_포인트차감() throws InterruptedException {
    Long userId = savedUsers.get(0).getId();
    pointService.charge(savedUsers.get(0).getId(), new BigDecimal(30));
    // 주문 1: 상품1, 단가 1원
    Long productId1 = savedProducts.get(0).getId();
    CreateOrderCommand orderCommand1 = createOrderCommand(userId, productId1, 1);

    // 주문 2: 상품2, 단가 4원
    Long productId2 = savedProducts.get(1).getId();
    CreateOrderCommand orderCommand2 = createOrderCommand(userId, productId2, 1);

    // 동시성 테스트: 상품1, 상품2를 각각 5개씩 병렬 주문
    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    AtomicInteger errorCount = new AtomicInteger();

    for (int i = 0; i < threadCount; i++) {
      final CreateOrderCommand command = (i % 2 == 0) ? orderCommand1 : orderCommand2;
      executor.submit(() -> {
        try {
          sut.createOrder(command);
        } catch (Exception e) {
          // 포인트 부족 등 실패 카운트
          errorCount.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    latch.await();

    // 재고 검증
    Stock stock1 = stockService.getStock(productId1);
    Stock stock2 = stockService.getStock(productId2);

    assertThat(stock1.getAvailable()).isEqualTo(5); // 상품1
    assertThat(stock2.getAvailable()).isEqualTo(5); // 상품2ㄴ

    // 포인트 검증: 초기 포인트 10 + 충전포인트 30, 상품1 단가1 + 상품2 단가4 → 성공 주문만 차감
    BigDecimal remainingPoint = pointService.getAmount(userId);
    assertThat(remainingPoint).isEqualByComparingTo(new BigDecimal(15));

    // 실패 건수 검증
    assertThat(errorCount.get()).isZero();
  }

  private CreateOrderCommand createOrderCommand(Long userId, Long productId, int quantity) {
    List<OrderCreateV1Dto.OrderItemRequest> items = List.of(
        new OrderCreateV1Dto.OrderItemRequest(productId, quantity)
    );
    OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
    return CreateOrderCommand.from(userId, request);
  }
}
