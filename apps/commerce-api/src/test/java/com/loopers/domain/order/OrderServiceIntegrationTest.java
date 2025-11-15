package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.loopers.domain.order.OrderAssertions.assertOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {
  @Autowired
  private OrderService orderService;

  @MockitoSpyBean
  private UserJpaRepository userJpaRepository;

  @MockitoSpyBean
  private BrandJpaRepository brandJpaRepository;
  @MockitoSpyBean
  private ProductJpaRepository productJpaRepository;
  @MockitoSpyBean
  private OrderJpaRepository orderJpaRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  List<Brand> savedBrands;
  List<Product> savedProducts;
  Order savedOrder;

  @BeforeEach
  void setup() {
    User user = User.create("user1", "user1@test.XXX", "1999-01-01", "F");
    savedUser = userJpaRepository.save(user);

    List<Brand> brandList = List.of(Brand.create("레이브", "레이브는 음악, 영화, 예술 등 다양한 문화에서 영감을 받아 경계 없고 자유분방한 스타일을 제안하는 패션 레이블입니다.")
        , Brand.create("마뗑킴", "마뗑킴은 트렌디하면서도 편안함을 더한 디자인을 선보입니다. 일상에서 조화롭게 적용할 수 있는 자연스러운 패션 문화를 지향합니다."));
    savedBrands = brandList.stream().map((brand) -> brandJpaRepository.save(brand)).toList();

    List<Product> productList = List.of(Product.create(savedBrands.get(0), "Wild Faith Rose Sweatshirt", Money.wons(80_000), 10)
        , Product.create(savedBrands.get(0), "Flower Pattern Fleece Jacket", Money.wons(80_000), 20)
        , Product.create(savedBrands.get(1), "Flower Pattern Fleece Jacket", Money.wons(80_000), 20)
    );
    savedProducts = productList.stream().map((product) -> productJpaRepository.save(product)).toList();

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
    void 성공_상품목록조회() {
      // arrange

      // act
      Page<Order> ordersPage = orderService.getOrders(savedUser.getId(), "latest", 0, 20);
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
    void 성공_존재하는_주문ID() {
      // arrange
      // act
      Order result = orderService.getOrder(savedOrder.getId());

      // assert
      assertOrder(result, savedOrder);
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, null이 반환된다.")
    @Test
    void 실패_존재하지_않는_주문ID() {
      // arrange

      // act
      Order result = orderService.getOrder((long) 99999);

      // assert
      assertThat(result).isNull();
    }
  }

  @DisplayName("주문 생성")
  @Nested
  class Save {
    @DisplayName("주문 생성을 한다.")
    @Test
    void 성공_주문생성() {
      // arrange
      List<OrderItem> orderItems = new ArrayList<>();
      orderItems.add(OrderItem.create(savedProducts.get(0).getId(), 2L, Money.wons(5_000)));
      Order order = Order.create(savedUser.getId(), OrderStatus.PENDING, Money.wons(10_000), orderItems);

      // act
      orderService.save(order);

      // assert
      assertAll(
          () -> verify(orderJpaRepository, times(1)).save(order)
      );
    }
  }
}
