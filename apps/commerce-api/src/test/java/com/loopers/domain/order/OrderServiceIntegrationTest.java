package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
  private OrderService sut;

  @MockitoSpyBean
  private UserRepository userRepository;
  @MockitoSpyBean
  private PointRepository pointRepository;

  @MockitoSpyBean
  private BrandRepository brandRepository;
  @MockitoSpyBean
  private ProductRepository productRepository;
  @MockitoSpyBean
  private OrderRepository orderRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  User savedUser;
  List<Brand> savedBrands;
  List<Product> savedProducts;
  Order savedOrder;

  @BeforeEach
  void setup() {
    savedUser = userRepository.save(UserFixture.createUser());
    pointRepository.save(Point.create(savedUser, BigDecimal.TEN));

    List<Brand> brandList = List.of(BrandFixture.createBrand(), BrandFixture.createBrand());
    savedBrands = brandRepository.saveAll(brandList);

    List<Product> productList = List.of(ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(0))
        , ProductFixture.createProduct(savedBrands.get(1)));
    savedProducts = productRepository.saveAll(productList);

    List<OrderItem> orderItems = new ArrayList<>();
    orderItems.add(OrderItem.create(productList.get(0).getId(), 2L, Money.wons(5_000)));
    Order order = Order.create(savedUser.getId(), orderItems);
    savedOrder = orderRepository.save(order);

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
      Long userId = savedUser.getId();
      // act
      Page<Order> ordersPage = sut.getOrders(userId, "latest", 0, 20);
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
      Long orderId = savedOrder.getId();
      // act
      Order result = sut.getOrder(orderId);

      // assert
      assertOrder(result, savedOrder);
    }

    @DisplayName("존재하지 않는 상품 ID를 주면, null이 반환된다.")
    @Test
    void 실패_존재하지_않는_주문ID() {
      // arrange
      Long orderId = (long) -1;
      // act
      Order result = sut.getOrder(orderId);
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
      Order order = Order.create(savedUser.getId(), orderItems);

      // act
      sut.save(order);

      // assert
      assertAll(
          () -> verify(orderRepository, times(1)).save(order)
      );
    }
  }
}
