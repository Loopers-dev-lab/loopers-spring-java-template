package com.loopers.interfaces.api.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    private static final String ENDPOINT_ORDER = "/api/v1/orders";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final TestFixture testFixture;

    @Autowired
    public OrderV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            TestFixture testFixture
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.testFixture = testFixture;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    class PlaceOrder {

        @DisplayName("주문 요청에 성공할 경우, 주문 정보를 응답으로 반환한다.")
        @Test
        void returnsOrderInfo_whenOrderIsSuccessful() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 2)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().orderId()).isNotNull(),
                    () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(20000L),
                    () -> assertThat(response.getBody().data().items()).hasSize(1)
            );
        }

        @DisplayName("쿠폰을 적용한 주문 요청에 성공할 경우, 할인된 주문 정보를 반환한다.")
        @Test
        void returnsDiscountedOrderInfo_whenCouponApplied() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Coupon coupon = testFixture.createFixedAmountCoupon(user, 1000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 2)),
                    coupon.getId(),
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(20000L),
                    () -> assertThat(response.getBody().data().discountAmount()).isEqualTo(1000L),
                    () -> assertThat(response.getBody().data().finalAmount()).isEqualTo(19000L)
            );
        }

        @DisplayName("여러 상품을 한 번에 주문할 경우, 모든 상품이 주문 항목에 포함된다.")
        @Test
        void returnsMultipleItems_whenOrderHasMultipleProducts() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product1 = testFixture.createProduct("Product A", 5000L, 100, brand);
            Product product2 = testFixture.createProduct("Product B", 8000L, 100, brand);
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(
                            new OrderV1Dto.OrderItemRequest(product1.getId(), 2),
                            new OrderV1Dto.OrderItemRequest(product2.getId(), 3)
                    ),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().items()).hasSize(2),
                    () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(34000L) // 5000*2 + 8000*3
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("존재하지 않는 사용자로 주문 요청 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistentuser");
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("존재하지 않는 상품으로 주문 요청 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(99999L, 1)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("재고가 부족한 상품 주문 요청 시, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenStockIsInsufficient() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 5); // 재고 5개
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 10)), // 10개 주문
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("재고가 0인 상품 주문 요청 시, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenProductIsOutOfStock() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createOutOfStockProduct(brand); // 재고 0개
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("주문 상품 목록이 비어있을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenItemsIsEmpty() {
            // arrange
            User user = testFixture.createUser("testuser01");
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(), // 빈 목록
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("수량이 0인 주문 요청 시, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenQuantityIsZero() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 0)),
                    null,
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("존재하지 않는 쿠폰으로 주문 요청 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenCouponDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);

            OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    99999L, // 존재하지 않는 쿠폰 ID
                    "POINT",
                    null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    class GetOrders {

        @DisplayName("주문 목록 조회에 성공할 경우, 주문 목록을 응답으로 반환한다.")
        @Test
        void returnsOrderList_whenOrdersExist() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 주문 2건 생성
            OrderV1Dto.PlaceOrderRequest orderRequest1 = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    null, "POINT", null
            );
            OrderV1Dto.PlaceOrderRequest orderRequest2 = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 2)),
                    null, "POINT", null
            );
            testRestTemplate.exchange(ENDPOINT_ORDER, HttpMethod.POST,
                    new HttpEntity<>(orderRequest1, headers),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {
                    });
            testRestTemplate.exchange(ENDPOINT_ORDER, HttpMethod.POST,
                    new HttpEntity<>(orderRequest2, headers),
                    new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {
                    });

            // act
            ParameterizedTypeReference<ApiResponse<List<OrderV1Dto.OrderResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).hasSize(2)
            );
        }

        @DisplayName("주문 내역이 없는 사용자의 주문 목록 조회 시, 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoOrdersExist() {
            // arrange
            User user = testFixture.createUser("testuser01");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<List<OrderV1Dto.OrderResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data()).isEmpty()
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // act
            ParameterizedTypeReference<ApiResponse<List<OrderV1Dto.OrderResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER,
                            HttpMethod.GET,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    class GetOrderDetail {

        @DisplayName("주문 상세 조회에 성공할 경우, 주문 정보를 응답으로 반환한다.")
        @Test
        void returnsOrderDetail_whenOrderExists() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 주문 생성
            OrderV1Dto.PlaceOrderRequest orderRequest = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 2)),
                    null, "POINT", null
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> createResponse =
                    testRestTemplate.exchange(ENDPOINT_ORDER, HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers),
                            new ParameterizedTypeReference<>() {
                            });
            Long orderId = createResponse.getBody().data().orderId();

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER + "/" + orderId,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId),
                    () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(20000L),
                    () -> assertThat(response.getBody().data().items()).hasSize(1)
            );
        }

        @DisplayName("존재하지 않는 주문 ID로 조회 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER + "/99999",
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("다른 사용자의 주문을 조회하려 할 경우, 403 Forbidden 응답을 반환한다.")
        @Test
        void returnsForbidden_whenAccessingOtherUsersOrder() {
            // arrange
            User user1 = testFixture.createUser("testuser01");
            User user2 = testFixture.createUser("testuser02");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user1.getId(), 100000L);

            // user1이 주문 생성
            HttpHeaders headers1 = new HttpHeaders();
            headers1.set("X-USER-ID", user1.getLoginId());
            headers1.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dto.PlaceOrderRequest orderRequest = new OrderV1Dto.PlaceOrderRequest(
                    List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                    null, "POINT", null
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> createResponse =
                    testRestTemplate.exchange(ENDPOINT_ORDER, HttpMethod.POST,
                            new HttpEntity<>(orderRequest, headers1),
                            new ParameterizedTypeReference<>() {
                            });
            Long orderId = createResponse.getBody().data().orderId();

            // user2가 user1의 주문 조회 시도
            HttpHeaders headers2 = new HttpHeaders();
            headers2.set("X-USER-ID", user2.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_ORDER + "/" + orderId,
                            HttpMethod.GET,
                            new HttpEntity<>(headers2),
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND)
            );
        }
    }
}
