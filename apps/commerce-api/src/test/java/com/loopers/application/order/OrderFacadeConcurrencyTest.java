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
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderCreateV1Dto;
import com.loopers.utils.DatabaseCleanUp;
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
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class OrderFacadeConcurrencyTest {
  @Autowired
  private OrderFacade sut;
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
  Order savedOrder;
  Stock savedStock;

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

    Stock stock = StockFixture.createStockWith(savedProducts.get(0).getId(), 10);
    savedStock = stockService.save(stock);
    stock = StockFixture.createStockWith(savedProducts.get(1).getId(), 10);
    savedStock = stockService.save(stock);

    List<OrderItem> orderItems = new ArrayList<>();
    orderItems.add(OrderItem.create(savedProducts.get(0).getId(), 2L, Money.wons(5_000)));
    Order order = Order.create(savedUsers.get(0).getId(), orderItems);
    savedOrder = orderService.save(order);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("동시에 주문해도 재고가 정상적으로 차감된다.")
  @Test
  void 성공_재고10_단가1원_쓰레드10() throws InterruptedException {
    Long productId = savedProducts.get(0).getId();
    List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
    items.add(new OrderCreateV1Dto.OrderItemRequest(productId, 1));
    OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
    CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUsers.get(0).getId(), request);


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

    Stock stock = stockService.findByProductId(productId);
    assertThat(stock.getAvailable()).isZero();
  }

//  @DisplayName("동시에 주문해도 재고가 정상적으로 차감된다.")
//  @Test
//  void 실패_재고10_단가4원_쓰레드10() throws InterruptedException {
//    Long userId = savedUsers.get(0).getId();
//    Long productId = savedProducts.get(1).getId();
//    List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
//    items.add(new OrderCreateV1Dto.OrderItemRequest(productId, 1));
//    OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
//    CreateOrderCommand orderCommand = CreateOrderCommand.from(userId, request);
//
//    AtomicInteger errorCount = new AtomicInteger();
//    int threadCount = 10;
//    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//    CountDownLatch latch = new CountDownLatch(threadCount);
//
//    for (int i = 0; i < threadCount; i++) {
//      executor.submit(() -> {
//        try {
//          sut.createOrder(orderCommand);
//        } catch (Exception e) {
//          System.out.println("실패: " + e.getMessage());
//          errorCount.getAndIncrement();
//        } finally {
//          latch.countDown();
//        }
//      });
//    }
//
//    latch.await();
//
//    Stock stock = stockService.findByProductId(productId);
//    assertThat(stock.getAvailable()).isEqualTo(0);
//    BigDecimal bigDecimal = pointService.getAmount(userId);
//    assertThat(bigDecimal).isEqualByComparingTo(new BigDecimal(2));
//  }

}
