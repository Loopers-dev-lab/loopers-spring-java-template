package com.loopers.interfaces.api.payment;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto;
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
class PaymentV1ApiE2ETest {

    private static final String ENDPOINT_PAYMENT = "/api/v1/payments";
    private static final String ENDPOINT_ORDER = "/api/v1/orders";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final TestFixture testFixture;

    @Autowired
    public PaymentV1ApiE2ETest(
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

    private Long createOrderAndGetId(User user, Product product) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", user.getLoginId());
        headers.setContentType(MediaType.APPLICATION_JSON);

        OrderV1Dto.PlaceOrderRequest orderRequest = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), 1)),
                null, "POINT", null
        );

        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(
                        ENDPOINT_ORDER,
                        HttpMethod.POST,
                        new HttpEntity<>(orderRequest, headers),
                        new ParameterizedTypeReference<>() {
                        }
                );

        return response.getBody().data().orderId();
    }

    @DisplayName("POST /api/v1/payments/point")
    @Nested
    class PayWithPoint {

        @DisplayName("포인트 결제 요청에 성공할 경우, 결제 정보를 응답으로 반환한다.")
        @Test
        void returnsPaymentInfo_whenPaymentIsSuccessful() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    orderId,
                    0L,
                    null,
                    "test-idempotency-key-001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().paymentId()).isNotNull(),
                    () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId),
                    () -> assertThat(response.getBody().data().paymentMethod()).isEqualTo("POINT")
            );
        }

        @DisplayName("멱등성 키가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenIdempotencyKeyIsMissing() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            String requestBody = """
                    {
                        "orderId": %d,
                        "discountAmount": 0
                    }
                    """.formatted(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
                            HttpMethod.POST,
                            new HttpEntity<>(requestBody, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    1L,
                    0L,
                    null,
                    "test-idempotency-key-001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
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

        @DisplayName("존재하지 않는 주문 ID로 결제 요청 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");
            testFixture.createPoint(user.getId(), 100000L);

            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    99999L, // 존재하지 않는 주문 ID
                    0L,
                    null,
                    "test-idempotency-key-001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
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

        @DisplayName("동일한 멱등성 키로 재요청 시, 동일한 결제 정보를 반환한다.")
        @Test
        void returnsSamePaymentInfo_whenIdempotencyKeyIsDuplicated() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            String idempotencyKey = "test-idempotency-key-duplicate";
            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    orderId,
                    0L,
                    null,
                    idempotencyKey
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 첫 번째 요청
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> firstResponse =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            new ParameterizedTypeReference<>() {
                            }
                    );

            // act - 두 번째 요청 (동일한 멱등성 키)
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> secondResponse =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            new ParameterizedTypeReference<>() {
                            }
                    );

            // assert
            assertAll(
                    () -> assertTrue(secondResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(secondResponse.getBody().data().paymentId())
                            .isEqualTo(firstResponse.getBody().data().paymentId()),
                    () -> assertThat(secondResponse.getBody().data().idempotencyKey())
                            .isEqualTo(idempotencyKey)
            );
        }
    }

    @DisplayName("POST /api/v1/payments/card")
    @Nested
    class PayWithCard {

        @DisplayName("카드 결제 요청에 성공할 경우, 결제 정보를 응답으로 반환한다.")
        @Test
        void returnsPaymentInfo_whenCardPaymentIsSuccessful() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            PaymentV1Dto.CardPaymentRequest request = new PaymentV1Dto.CardPaymentRequest(
                    orderId,
                    "SAMSUNG",
                    "1234-5678-9012-3456",
                    0L,
                    null,
                    "test-card-idempotency-key-001"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/card",
                            HttpMethod.POST,
                            new HttpEntity<>(request, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId),
                    () -> assertThat(response.getBody().data().paymentMethod()).isEqualTo("PG_CARD")
            );
        }

        @DisplayName("카드 번호가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenCardNoIsMissing() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            String requestBody = """
                    {
                        "orderId": %d,
                        "cardType": "SAMSUNG",
                        "discountAmount": 0,
                        "idempotencyKey": "test-key"
                    }
                    """.formatted(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/card",
                            HttpMethod.POST,
                            new HttpEntity<>(requestBody, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("카드 타입이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenCardTypeIsMissing() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            String requestBody = """
                    {
                        "orderId": %d,
                        "cardNo": "1234-5678-9012-3456",
                        "discountAmount": 0,
                        "idempotencyKey": "test-key"
                    }
                    """.formatted(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/card",
                            HttpMethod.POST,
                            new HttpEntity<>(requestBody, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/payments/{paymentId}")
    @Nested
    class GetPaymentDetail {

        @DisplayName("결제 정보 조회에 성공할 경우, 결제 상세 정보를 반환한다.")
        @Test
        void returnsPaymentDetail_whenPaymentExists() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            // 결제 생성
            PaymentV1Dto.PointPaymentRequest paymentRequest = new PaymentV1Dto.PointPaymentRequest(
                    orderId, 0L, null, "test-key-detail"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentResponse>> createResponse =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/point",
                            HttpMethod.POST,
                            new HttpEntity<>(paymentRequest, headers),
                            new ParameterizedTypeReference<>() {
                            }
                    );
            Long paymentId = createResponse.getBody().data().paymentId();

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/" + paymentId,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().paymentId()).isEqualTo(paymentId),
                    () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId)
            );
        }

        @DisplayName("존재하지 않는 결제 ID로 조회 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenPaymentDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/99999",
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
    }

    @DisplayName("GET /api/v1/payments/orders/{orderId}")
    @Nested
    class GetPaymentByOrderId {

        @DisplayName("주문별 결제 정보 조회에 성공할 경우, 결제 상세 정보를 반환한다.")
        @Test
        void returnsPaymentDetail_whenPaymentExistsForOrder() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            // 결제 생성
            PaymentV1Dto.PointPaymentRequest paymentRequest = new PaymentV1Dto.PointPaymentRequest(
                    orderId, 0L, null, "test-key-by-order"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            testRestTemplate.exchange(
                    ENDPOINT_PAYMENT + "/point",
                    HttpMethod.POST,
                    new HttpEntity<>(paymentRequest, headers),
                    new ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentResponse>>() {
                    }
            );

            // act
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/orders/" + orderId,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId)
            );
        }

        @DisplayName("결제가 없는 주문 ID로 조회 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenNoPaymentForOrder() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand, 10000L, 100);
            testFixture.createPoint(user.getId(), 100000L);
            Long orderId = createOrderAndGetId(user, product);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act (결제 없이 주문별 결제 조회)
            ParameterizedTypeReference<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PaymentV1Dto.PaymentDetailResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_PAYMENT + "/orders/" + orderId,
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
    }
}
