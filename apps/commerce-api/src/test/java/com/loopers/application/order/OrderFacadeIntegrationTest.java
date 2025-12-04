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
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class OrderFacadeIntegrationTest {
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
        ProductFixture.createProductWith("product1", Money.wons(8)),
        ProductFixture.createProductWith("product2", Money.wons(4)),
        ProductFixture.createProduct(savedBrands.get(1))
    ));
    // 재고 생성
    stockService.save(StockFixture.createStockWith(savedProducts.get(0).getId(), 10));
    stockService.save(StockFixture.createStockWith(savedProducts.get(1).getId(), 10));

    List<OrderItem> orderItems = new ArrayList<>();
    orderItems.add(OrderItem.create(savedProducts.get(0).getId(), 2L, Money.wons(5_000)));
    Order order = Order.create(savedUsers.get(0).getId(), orderItems);
    savedOrder = orderService.save(order);

  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("주문목록을 조회할 때,")
  @Nested
  class GetList {
    @DisplayName("페이징 처리되어, 초기설정시 size=20, sort=최신순으로 목록이 조회된다.")
    @Test
    void 성공_주문목록조회() {
      Long userId = savedUsers.get(0).getId();
      // act
      Page<Order> ordersPage = sut.getOrderList(userId, "latest", 0, 20);
      List<Order> orders = ordersPage.getContent();
      // assert
      assertThat(orders).isNotEmpty().hasSize(1);
    }
  }

  @DisplayName("주문을 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 주문 ID를 주면, 해당 주문 정보를 반환한다.")
    @Test
    void 성공_존재하는_상품ID() {
      // arrange
      Long orderId = savedOrder.getId();
      // act
      OrderInfo result = sut.getOrderDetail(orderId);

      // assert
      assertThat(result.id()).isEqualTo(savedOrder.getId());
      assertThat(result.status()).isEqualTo(savedOrder.getStatus().toString());
      assertThat(result.totalPrice()).isEqualByComparingTo(savedOrder.getTotalPrice().getAmount());
    }

    @DisplayName("존재하지 않는 주문 ID를 주면, 예외가 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange
      Long orderId = (long) -1;
      // act
      // assert
      assertThrows(CoreException.class, () -> {
        sut.getOrderDetail(orderId);
      });
    }
  }

  @DisplayName("주문을 생성할 때,")
  @Nested
  class Post {
    @DisplayName("주문 성공 시, 모든 처리는 정상 반영되어야 한다.")
    @Test
    void 성공_단건주문생성() {
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 1));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUsers.get(0).getId(), request);
      // act
      OrderInfo savedOrder = sut.createOrder(orderCommand);
      // assert
      assertThat(savedOrder).isNotNull();
      assertThat(savedOrder.totalPrice()).isEqualByComparingTo(savedProducts.get(0).getPrice().getAmount());
      assertThat(savedOrder.orderItemInfo()).hasSize(1);
    }

    @DisplayName("재고가 존재하지 않거나 부족할 경우 주문은 실패해야 한다. 모두 롤백")
    @Test
    void 실패_재고없음오류() {
      long productId = savedProducts.get(1).getId();
      long quantity = 20L;
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(productId, quantity));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUsers.get(0).getId(), request);
      // act
      // assert
      assertThrows(CoreException.class, () -> sut.createOrder(orderCommand)).getErrorType().equals(ErrorType.INSUFFICIENT_STOCK);
      Stock deductedStock = stockService.getStock(productId);
      assertThat(deductedStock.getAvailable()).isEqualTo(10);
    }

    @DisplayName("주문 시 유저의 포인트 잔액이 부족할 경우 주문은 실패해야 한다. 모두 롤백")
    @Test
    void 실패_포인트부족오류() {
      long productId = savedProducts.get(1).getId();
      long quantity = 3L;
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(productId, quantity));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUsers.get(0).getId(), request);
      // act
      // assert
      CoreException actualException = assertThrows(CoreException.class,
          () -> sut.createOrder(orderCommand));
      assertThat(actualException.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_POINT);
      Stock deductedStock = stockService.getStock(productId);
      assertThat(deductedStock.getAvailable()).isEqualTo(10);
    }
  }

}
