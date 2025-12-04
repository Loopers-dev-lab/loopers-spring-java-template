package com.loopers.interfaces.api;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockFixture;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.order.OrderCreateV1Dto;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderV1ApiE2ETest {

  private static final String ENDPOINT = "/api/v1/orders";
  private static final Function<String, String> ENDPOINT_GET = id -> "/api/v1/orders/" + id;

  private final TestRestTemplate testRestTemplate;
  private final UserService userService;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  private OrderFacade sut;
  @MockitoSpyBean
  private UserRepository userRepository;
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
  List<User> savedUsers;
  List<Brand> savedBrands;
  List<Product> savedProducts;

  @Autowired
  public OrderV1ApiE2ETest(
      TestRestTemplate testRestTemplate,
      UserService userService,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.userService = userService;
    this.databaseCleanUp = databaseCleanUp;
  }

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

  @DisplayName("주문 생성")
  @Nested
  class Post {
    @DisplayName("주문 생성-E2E테스트1")
    @Test
    void 성공_주문생성() {
      //given
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", savedUsers.get(0).getId().toString());
      List<OrderCreateV1Dto.OrderItemRequest> items = List.of(
          new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 1L)
      );
      OrderCreateV1Dto.OrderRequest req = new OrderCreateV1Dto.OrderRequest(items);
      //when
      ParameterizedTypeReference<ApiResponse<OrderCreateV1Dto.OrderResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<OrderCreateV1Dto.OrderResponse>> res = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(req, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(res.getBody().data()).isNotNull();
      assertThat(res.getBody().data().totalPrice()).isEqualByComparingTo(savedProducts.get(0).getPrice().getAmount());
    }
  }

  @DisplayName("주문 정보 조회")
  @Nested
  class Get {
    @DisplayName("E2E테스트1")
    @Test
    void 성공_주문정보조회() {
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", savedUsers.get(0).getId().toString());
      List<OrderCreateV1Dto.OrderItemRequest> orderitems = List.of(
          new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 1L)
      );
      OrderCreateV1Dto.OrderRequest orderRequest = new OrderCreateV1Dto.OrderRequest(orderitems);
      //when
      ParameterizedTypeReference<ApiResponse<OrderCreateV1Dto.OrderResponse>> orderResType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<OrderCreateV1Dto.OrderResponse>> orderRes = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(orderRequest, headers), orderResType);


      //given
      List<OrderCreateV1Dto.OrderItemRequest> items = List.of(
          new OrderCreateV1Dto.OrderItemRequest(savedProducts.get(0).getId(), 1L)
      );
      OrderCreateV1Dto.OrderRequest req = new OrderCreateV1Dto.OrderRequest(items);

      //when
      String url = ENDPOINT_GET.apply(savedUsers.get(0).getId().toString());
      ParameterizedTypeReference<ApiResponse<OrderCreateV1Dto.OrderResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<OrderCreateV1Dto.OrderResponse>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data()).isNotNull();
      assertThat(res.getBody().data().totalPrice()).isEqualByComparingTo(savedProducts.get(0).getPrice().getAmount());
    }

    @DisplayName("E2E테스트2")
    @Test
    void 실패_존재하지_않는_주문ID() {
      //given
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", savedUsers.get(0).getId().toString());
      String orderId = "-1";

      //when
      String url = ENDPOINT_GET.apply(orderId);
      ParameterizedTypeReference<ApiResponse<OrderCreateV1Dto.OrderResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<OrderCreateV1Dto.OrderResponse>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

  }
}
