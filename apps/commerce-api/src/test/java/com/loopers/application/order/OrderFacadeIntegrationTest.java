package com.loopers.application.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.order.OrderCreateV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
class OrderFacadeIntegrationTest {
  @Autowired
  private OrderFacade orderFacade;
  @MockitoSpyBean
  private UserJpaRepository userJpaRepository;
  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;
  @MockitoSpyBean
  private ProductService productService;

  @MockitoSpyBean
  private OrderJpaRepository orderJpaRepository;
  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  List<Product> savedProducts;
  Order savedOrder;

  @BeforeEach
  void setup() {
    // arrange
    User user = User.create("user1", "user1@test.XXX", "1999-01-01", "F");
    savedUser = userJpaRepository.save(user);
    List<Brand> brandList = List.of(Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.")
        , Brand.create("마뗑킴", "마뗑킴은 트렌디하면서도 편안함을 더한 디자인을 선보입니다. 일상에서 조화롭게 적용할 수 있는 자연스러운 패션 문화를 지향합니다."));
    List<Brand> savedBrandList = brandList.stream().map((brand) -> brandJpaRepository.save(brand)).toList();

    List<Product> productList = List.of(Product.create(savedBrandList.get(0), "Wild Faith Rose Sweatshirt", Money.wons(8), 10)
        , Product.create(savedBrandList.get(0), "Flower Pattern Fleece Jacket", Money.wons(4), 10)
        , Product.create(savedBrandList.get(1), "Flower Pattern Fleece Jacket", Money.wons(178_000), 20)
    );
    savedProducts = productService.save(productList);
    List<OrderItem> orderItems = new ArrayList<>();
    orderItems.add(OrderItem.create(productList.get(0).getId(), 2L, Money.wons(5_000)));
    Order order = Order.create(savedUser.getId(), OrderStatus.PENDING, Money.wons(10_000), orderItems);
    savedOrder = orderJpaRepository.save(order);

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
      // act
      Page<Order> ordersPage = orderFacade.getOrderList(savedUser.getId(), "latest", 0, 20);
      List<Order> orders = ordersPage.getContent();
      // assert
      assertThat(orders).isNotEmpty().hasSize(1);
    }
  }

  @DisplayName("주문을 조회할 때,")
  @Nested
  class Get {
    @DisplayName("존재하는 상품 ID를 주면, 해당 상품 정보를 반환한다.")
    @Test
    void 성공_존재하는_상품ID() {
      // arrange
      // act
      OrderInfo result = orderFacade.getOrderDetail(savedOrder.getId());

      // assert
      assertThat(result.id()).isEqualTo(savedOrder.getId());
      assertThat(result.status()).isEqualTo(savedOrder.getStatus().toString());
      assertThat(result.totalPrice()).isEqualByComparingTo(savedOrder.getTotalPrice().getAmount());
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, 예외가 반환된다.")
    @Test
    void 실패_존재하지_않는_상품ID() {
      // arrange
      // act
      // assert
      assertThrows(CoreException.class, () -> {
        orderFacade.getOrderDetail(0L);
      });
    }
  }

  @DisplayName("주문을 생성할 때,")
  @Nested
  class Post {
    @DisplayName("10재고가 있는 8원의 상품 1건 주문시, 기본 포인트10으로 결제가 된다.")
    @Test
    void 성공_단건주문생성() {
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 1));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUser.getId(), request);
      // act
      OrderInfo savedOrder = orderFacade.createOrder(orderCommand);
      // assert
      assertThat(savedOrder).isNotNull();
      assertThat(savedOrder.totalPrice()).isEqualByComparingTo(savedProducts.get(0).getPrice().getAmount());
      assertThat(savedOrder.orderItemInfo()).hasSize(1);
    }

    @DisplayName("10재고가 있는 8원의 상품 20건 주문시, 재고 없음 오류가 발생한다.")
    @Test
    void 실패_재고없음오류() {
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 20));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUser.getId(), request);
      // act
      // assert
      assertThrows(CoreException.class, () -> orderFacade.createOrder(orderCommand)).getErrorType().equals(ErrorType.INSUFFICIENT_STOCK);
    }

    @DisplayName("10재고가 있는 4원의 상품 3건 주문시, 포인트 부족 오류가 발생한다.")
    @Test
    void 실패_포인트부족오류() {
      long productId = savedProducts.get(1).getId();
      long quantity = 3L;
      List<OrderCreateV1Dto.OrderItemRequest> items = new ArrayList<>();
      items.add(new OrderCreateV1Dto.OrderItemRequest(productId, quantity));
      OrderCreateV1Dto.OrderRequest request = new OrderCreateV1Dto.OrderRequest(items);
      CreateOrderCommand orderCommand = CreateOrderCommand.from(savedUser.getId(), request);
      // act
      // assert
      CoreException actualException = assertThrows(CoreException.class,
          () -> orderFacade.createOrder(orderCommand));
      assertThat(actualException.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_POINT);
      Product deductedProduct = productService.getProduct(productId);
      assertThat(deductedProduct.getStock()).isEqualTo(10);
    }
  }

}
