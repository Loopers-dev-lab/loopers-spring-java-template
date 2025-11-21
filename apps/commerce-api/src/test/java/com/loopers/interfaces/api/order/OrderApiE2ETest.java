package com.loopers.interfaces.api.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderPrice;
import com.loopers.domain.quantity.Quantity;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointAmount;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderDetailResponse;
import com.loopers.interfaces.api.order.OrderDto.OrderListResponse;
import com.loopers.interfaces.api.support.ApiHeaders;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiE2ETest {

  private static final String BASE_URL = "/api/v1/orders";
  private static final LocalDate BIRTH_DATE_1990_01_01 = LocalDate.of(1990, 1, 1);
  private static final LocalDateTime FIRST_ORDER_AT_2025_10_30_10_00 = LocalDateTime.of(2025, 10, 30, 10, 0, 0);
  private static final LocalDateTime SECOND_ORDER_AT_2025_10_30_11_00 = LocalDateTime.of(2025, 10, 30, 11, 0, 0);
  private static final ParameterizedTypeReference<ApiResponse<OrderListResponse>> ORDER_LIST_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<ApiResponse<OrderDetailResponse>> ORDER_DETAIL_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final TestRestTemplate testRestTemplate;
  private final OrderJpaRepository orderJpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final BrandJpaRepository brandJpaRepository;
  private final ProductJpaRepository productJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  OrderApiE2ETest(
      TestRestTemplate testRestTemplate,
      OrderJpaRepository orderJpaRepository,
      UserJpaRepository userJpaRepository,
      BrandJpaRepository brandJpaRepository,
      ProductJpaRepository productJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.orderJpaRepository = orderJpaRepository;
    this.userJpaRepository = userJpaRepository;
    this.brandJpaRepository = brandJpaRepository;
    this.productJpaRepository = productJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  @DisplayName("GET /api/v1/orders")
  class GetOrders {

    @Test
    @DisplayName("사용자의 주문 목록과 페이징 정보를 반환한다")
    void returnsOrdersWithPagination_whenValidUserId() {
      User user = saveUser("user123", "user@example.com");
      Brand brand = saveBrand("나이키");
      Product product = saveProduct("운동화", 30000L, 50L, brand.getId());

      Order first = Order.of(user.getId(), OrderStatus.PENDING, 30000L, FIRST_ORDER_AT_2025_10_30_10_00);
      addOrderItem(first, product.getId(), "운동화", 1L, 30000L);
      orderJpaRepository.save(first);

      Order second = Order.of(user.getId(), OrderStatus.COMPLETED, 50000L, SECOND_ORDER_AT_2025_10_30_11_00);
      addOrderItem(second, product.getId(), "운동화", 2L, 25000L);
      orderJpaRepository.save(second);

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user.getId().toString());

      ResponseEntity<ApiResponse<OrderListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("page", 0)
                  .queryParam("size", 20)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_LIST_RESPONSE_TYPE
          );

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data().orders())
              .hasSize(2)
              .element(0).extracting("orderId", "totalAmount", "status", "itemCount")
              .containsExactly(second.getId(), 50000L, OrderStatus.COMPLETED, 1),
          () -> assertThat(response.getBody().data().orders())
              .element(1).extracting("orderId", "totalAmount", "status", "itemCount")
              .containsExactly(first.getId(), 30000L, OrderStatus.PENDING, 1)
      );
    }

    @Test
    @DisplayName("페이지네이션이 올바르게 동작한다")
    void returnsPaginatedOrders_whenPageSizeIsSet() {
      User user = saveUser("user123", "user@example.com");
      Brand brand = saveBrand("나이키");
      Product product = saveProduct("운동화", 30000L, 50L, brand.getId());

      for (int i = 0; i < 3; i++) {
        Order order = Order.of(user.getId(), OrderStatus.PENDING, 30000L, FIRST_ORDER_AT_2025_10_30_10_00);
        addOrderItem(order, product.getId(), "운동화", 1L, 30000L);
        orderJpaRepository.save(order);
      }

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user.getId().toString());

      ResponseEntity<ApiResponse<OrderListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("page", 0)
                  .queryParam("size", 2)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_LIST_RESPONSE_TYPE
          );

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data().orders()).hasSize(2),
          () -> assertThat(response.getBody().data().pageInfo())
              .extracting("totalElements", "currentPage", "totalPages", "pageSize")
              .containsExactly(3L, 0, 2, 2)
      );
    }

    @Test
    @DisplayName("다른 사용자의 주문은 조회되지 않는다")
    void returnsOnlyOwnOrders_whenMultipleUsersExist() {
      User user1 = saveUser("user1", "user1@example.com");
      User user2 = saveUser("user2", "user2@example.com");
      Brand brand = saveBrand("나이키");
      Product product = saveProduct("운동화", 30000L, 50L, brand.getId());

      Order order1 = Order.of(user1.getId(), OrderStatus.PENDING, 30000L, FIRST_ORDER_AT_2025_10_30_10_00);
      addOrderItem(order1, product.getId(), "운동화", 1L, 30000L);
      orderJpaRepository.save(order1);

      Order order2 = Order.of(user2.getId(), OrderStatus.PENDING, 50000L, FIRST_ORDER_AT_2025_10_30_10_00);
      addOrderItem(order2, product.getId(), "운동화", 2L, 25000L);
      orderJpaRepository.save(order2);

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user1.getId().toString());

      ResponseEntity<ApiResponse<OrderListResponse>> response =
          testRestTemplate.exchange(
              UriComponentsBuilder.fromUriString(BASE_URL)
                  .queryParam("page", 0)
                  .queryParam("size", 20)
                  .build()
                  .toUri(),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_LIST_RESPONSE_TYPE
          );

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data().orders())
              .hasSize(1)
              .element(0).extracting("orderId")
              .isEqualTo(order1.getId())
      );
    }
  }

  @Nested
  @DisplayName("GET /api/v1/orders/{orderId}")
  class GetOrderDetail {

    @Test
    @DisplayName("주문 상세 정보를 반환한다")
    void returnsOrderDetail_whenValidOrderId() {
      User user = saveUser("user123", "user@example.com");
      Brand brand = saveBrand("나이키");
      Product product1 = saveProduct("운동화", 30000L, 50L, brand.getId());
      Product product2 = saveProduct("슬리퍼", 20000L, 30L, brand.getId());

      Order order = Order.of(user.getId(), OrderStatus.PENDING, 50000L, FIRST_ORDER_AT_2025_10_30_10_00);
      addOrderItem(order, product1.getId(), "운동화", 1L, 30000L);
      addOrderItem(order, product2.getId(), "슬리퍼", 1L, 20000L);
      orderJpaRepository.save(order);

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user.getId().toString());

      ResponseEntity<ApiResponse<OrderDetailResponse>> response =
          testRestTemplate.exchange(
              BASE_URL + "/" + order.getId(),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_DETAIL_RESPONSE_TYPE
          );


      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data())
              .extracting("orderId", "status", "totalAmount")
              .containsExactly(order.getId(), OrderStatus.PENDING, 50000L),
          () -> assertThat(response.getBody().data().items())
              .hasSize(2)
              .extracting("productId", "productName", "quantity", "price")
              .containsExactlyInAnyOrder(
                  tuple(product1.getId(), "운동화", 1L, 30000L),
                  tuple(product2.getId(), "슬리퍼", 1L, 20000L)
              )
      );
    }

    @Test
    @DisplayName("타인의 주문은 조회할 수 없다 (403 Forbidden)")
    void returnsForbidden_whenAccessingOthersOrder() {
      User user1 = saveUser("user1", "user1@example.com");
      User user2 = saveUser("user2", "user2@example.com");
      Brand brand = saveBrand("나이키");
      Product product = saveProduct("운동화", 30000L, 50L, brand.getId());

      Order order = Order.of(user1.getId(), OrderStatus.PENDING, 30000L, FIRST_ORDER_AT_2025_10_30_10_00);
      addOrderItem(order, product.getId(), "운동화", 1L, 30000L);
      orderJpaRepository.save(order);

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user2.getId().toString());

      ResponseEntity<ApiResponse<OrderDetailResponse>> response =
          testRestTemplate.exchange(
              BASE_URL + "/" + order.getId(),
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_DETAIL_RESPONSE_TYPE
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 404 Not Found를 반환한다")
    void returnsNotFound_whenOrderNotExists() {
      User user = saveUser("user123", "user@example.com");

      HttpHeaders headers = new HttpHeaders();
      headers.set(ApiHeaders.USER_ID, user.getId().toString());

      ResponseEntity<ApiResponse<OrderDetailResponse>> response =
          testRestTemplate.exchange(
              BASE_URL + "/999999",
              HttpMethod.GET,
              new HttpEntity<>(headers),
              ORDER_DETAIL_RESPONSE_TYPE
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }

  private User saveUser(String loginId, String email) {
    LocalDate currentDate = LocalDate.of(2025, 10, 30);
    User user = User.of(loginId, email, BIRTH_DATE_1990_01_01, Gender.MALE, currentDate);
    return userJpaRepository.save(user);
  }

  private Brand saveBrand(String name) {
    Brand brand = Brand.of(name, name + " 브랜드");
    return brandJpaRepository.save(brand);
  }

  private Product saveProduct(String name, Long price, Long stock, Long brandId) {
    Product product = Product.of(name, Money.of(price), name + " 설명", Stock.of(stock), brandId);
    return productJpaRepository.save(product);
  }

  private void addOrderItem(Order order, Long productId, String productName, Long quantity,
      Long price) {
    OrderItem item = OrderItem.of(productId, productName, Quantity.of(quantity),
        OrderPrice.of(price));
    order.addItem(item);
  }
}
