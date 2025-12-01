package com.loopers.application.api.payment;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.*;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static com.loopers.application.api.order.OrderV1Dto.OrderRequest;
import static com.loopers.application.api.order.OrderV1Dto.OrderResponse;
import static com.loopers.application.api.payment.PaymentV1Dto.PaymentRequest;
import static com.loopers.application.api.payment.PaymentV1Dto.PaymentResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("결제 API")
class PaymentV1ApiTest extends ApiIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PgClient pgClient;

    private String userIdentifier;
    private String orderId;
    private String productId;

    @Nested
    @DisplayName("POST /api/v1/payments/orders/{orderId} - 결제 요청")
    class CreatePayment {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            @BeforeEach
            void setUp() {
                // 사용자 생성
                User user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), new UserIdentifier("testuser123"))
                                .set(field(User::getEmail), new UserEmail("testuser@example.com"))
                                .create()
                );
                userIdentifier = user.getIdentifier().value();

                // 사용자 포인트 생성
                userPointRepository.save(
                        Instancio.of(UserPoint.class)
                                .set(field("id"), UserPointId.empty())
                                .set(field("userId"), user.getId())
                                .set(field("balance"), new UserPointBalance(new BigDecimal(100_000)))
                                .create()
                );

                // 브랜드 생성
                Brand brand = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                );

                // 상품 생성
                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getName), new ProductName("MacBook Pro"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal("1500000")))
                                .set(field(Product::getStock), new ProductStock(100L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .create()
                );
                productId = product.getId().value();

                // 주문 생성 (API 호출)
                String orderEndPoint = "/api/v1/orders";
                HttpHeaders orderHeaders = new HttpHeaders();
                orderHeaders.set("X-USER-ID", userIdentifier);
                orderHeaders.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderRequest.OrderItemRequest(productId, 1L))
                );

                HttpEntity<OrderRequest> orderHttpEntity = new HttpEntity<>(orderRequest, orderHeaders);
                ParameterizedTypeReference<ApiResponse<OrderResponse>> orderResponseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<OrderResponse>> orderResponse =
                        testRestTemplate.exchange(orderEndPoint, HttpMethod.POST, orderHttpEntity, orderResponseType);

                orderId = orderResponse.getBody().data().orderId();

                // PG 클라이언트 Mock 설정
                when(pgClient.pay(any(), anyString()))
                        .thenReturn(new PgPayment(new TransactionKey("TXN_" + System.nanoTime())));
            }

            @Test
            @DisplayName("Status 200 - 결제 성공")
            void status200() {
                // Given
                String endPoint = "/api/v1/payments/orders/" + orderId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                headers.setContentType(MediaType.APPLICATION_JSON);

                PaymentRequest request = new PaymentRequest(
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<PaymentResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // When
                ResponseEntity<ApiResponse<PaymentResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertSoftly(softly -> {
                    softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    softly.assertThat(response.getBody()).isNotNull();
                    softly.assertThat(response.getBody().data()).isNotNull();
                    softly.assertThat(response.getBody().data().paymentId()).isNotBlank();
                });
            }
        }

        @Nested
        @DisplayName("X-USER-ID 헤더가 없을 경우")
        class X_USER_ID_헤더가_없을_경우 {

            @BeforeEach
            void setUp() {
                // 임시 주문 ID (실제로는 사용되지 않음)
                orderId = "temp-order-123";
            }

            @Test
            @DisplayName("Status 400")
            void status400() {
                // Given
                String endPoint = "/api/v1/payments/orders/" + orderId;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                PaymentRequest request = new PaymentRequest(
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // When
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 사용자로 요청할 경우")
        class 존재하지_않는_사용자로_요청 {

            @BeforeEach
            void setUp() {
                // 임시 주문 ID (실제로는 사용되지 않음)
                orderId = "temp-order-456";
            }

            @Test
            @DisplayName("Status 404")
            void status404() {
                // Given
                String endPoint = "/api/v1/payments/orders/" + orderId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-USER-ID", "nonExistentUser");
                headers.setContentType(MediaType.APPLICATION_JSON);

                PaymentRequest request = new PaymentRequest(
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                HttpEntity<PaymentRequest> httpEntity = new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // When
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/callback - PG 결제 콜백")
    class PgCallback {

        private String transactionKey;
        private String orderId;
        private String userId;
        private Payment payment;

        @Nested
        @DisplayName("결제 성공 콜백인 경우")
        class 결제_성공_콜백 {

            @BeforeEach
            void setUp() {
                // 사용자 생성
                User user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), new UserIdentifier("callbackuser"))
                                .set(field(User::getEmail), new UserEmail("callbackuser@example.com"))
                                .create()
                );
                userId = user.getId().value();

                // 사용자 포인트 생성
                userPointRepository.save(
                        Instancio.of(UserPoint.class)
                                .set(field("id"), UserPointId.empty())
                                .set(field("userId"), user.getId())
                                .set(field("balance"), new UserPointBalance(new BigDecimal(100_000)))
                                .create()
                );

                // 브랜드 및 상품 생성
                Brand brand = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                );

                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getName), new ProductName("Test Product"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal("50000")))
                                .set(field(Product::getStock), new ProductStock(100L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .create()
                );

                // 주문 생성 (API 호출)
                String orderEndPoint = "/api/v1/orders";
                HttpHeaders orderHeaders = new HttpHeaders();
                orderHeaders.set("X-USER-ID", user.getIdentifier().value());
                orderHeaders.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderRequest.OrderItemRequest(product.getId().value(), 1L))
                );

                HttpEntity<OrderRequest> orderHttpEntity = new HttpEntity<>(orderRequest, orderHeaders);
                ParameterizedTypeReference<ApiResponse<OrderResponse>> orderResponseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<OrderResponse>> orderResponse =
                        testRestTemplate.exchange(orderEndPoint, HttpMethod.POST, orderHttpEntity, orderResponseType);

                orderId = orderResponse.getBody().data().orderId();

                // Order 객체에서 OrderKey 조회
                Order order = orderRepository.getById(new com.loopers.core.domain.order.vo.OrderId(orderId));
                com.loopers.core.domain.order.vo.OrderKey orderKey = order.getOrderKey();

                // 결제 생성 (API 호출)
                String paymentEndPoint = "/api/v1/payments/orders/" + orderId;
                HttpHeaders paymentHeaders = new HttpHeaders();
                paymentHeaders.set("X-USER-ID", user.getIdentifier().value());
                paymentHeaders.setContentType(MediaType.APPLICATION_JSON);

                PaymentRequest paymentRequest = new PaymentRequest("CREDIT", "1234567890123456", null);

                transactionKey = "TXN_" + System.nanoTime();
                when(pgClient.pay(any(), anyString()))
                        .thenReturn(new PgPayment(new TransactionKey(transactionKey)));

                HttpEntity<PaymentRequest> paymentHttpEntity = new HttpEntity<>(paymentRequest, paymentHeaders);
                ParameterizedTypeReference<ApiResponse<PaymentResponse>> paymentResponseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<PaymentResponse>> paymentResponse =
                        testRestTemplate.exchange(paymentEndPoint, HttpMethod.POST, paymentHttpEntity, paymentResponseType);

                // 생성된 결제 객체 조회
                payment = paymentRepository.findBy(orderKey, PaymentStatus.PENDING)
                        .orElseThrow();
            }

            @Test
            @DisplayName("Status 200 - 결제 성공 처리")
            void status200_success() {
                // Given
                String endPoint = "/api/v1/payments/callback";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest request =
                        new com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest(
                                transactionKey,
                                orderId,
                                "CREDIT",
                                "1234567890123456",
                                50000L,
                                "SUCCESS",
                                null
                        );

                HttpEntity<com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest> httpEntity =
                        new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // When
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        @Nested
        @DisplayName("결제 실패 콜백인 경우")
        class 결제_실패_콜백 {

            @BeforeEach
            void setUp() {
                // 사용자 생성
                User user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), new UserIdentifier("failuser"))
                                .set(field(User::getEmail), new UserEmail("failuser@example.com"))
                                .create()
                );

                // 사용자 포인트 생성
                userPointRepository.save(
                        Instancio.of(UserPoint.class)
                                .set(field("id"), UserPointId.empty())
                                .set(field("userId"), user.getId())
                                .set(field("balance"), new UserPointBalance(new BigDecimal(100_000)))
                                .create()
                );

                // 브랜드 및 상품 생성
                Brand brand = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                );

                Product product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), brand.getId())
                                .set(field(Product::getName), new ProductName("Test Product"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal("50000")))
                                .set(field(Product::getStock), new ProductStock(100L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .create()
                );

                // 주문 생성 (API 호출)
                String orderEndPoint = "/api/v1/orders";
                HttpHeaders orderHeaders = new HttpHeaders();
                orderHeaders.set("X-USER-ID", user.getIdentifier().value());
                orderHeaders.setContentType(MediaType.APPLICATION_JSON);

                OrderRequest orderRequest = new OrderRequest(
                        List.of(new OrderRequest.OrderItemRequest(product.getId().value(), 1L))
                );

                HttpEntity<OrderRequest> orderHttpEntity = new HttpEntity<>(orderRequest, orderHeaders);
                ParameterizedTypeReference<ApiResponse<OrderResponse>> orderResponseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<OrderResponse>> orderResponse =
                        testRestTemplate.exchange(orderEndPoint, HttpMethod.POST, orderHttpEntity, orderResponseType);

                orderId = orderResponse.getBody().data().orderId();

                // Order 객체에서 OrderKey 조회
                Order order = orderRepository.getById(new com.loopers.core.domain.order.vo.OrderId(orderId));

                // 결제 생성 (API 호출)
                String paymentEndPoint = "/api/v1/payments/orders/" + orderId;
                HttpHeaders paymentHeaders = new HttpHeaders();
                paymentHeaders.set("X-USER-ID", user.getIdentifier().value());
                paymentHeaders.setContentType(MediaType.APPLICATION_JSON);

                PaymentRequest paymentRequest = new PaymentRequest("CREDIT", "1234567890123456", null);

                transactionKey = "TXN_FAIL_" + System.nanoTime();
                when(pgClient.pay(any(), anyString()))
                        .thenReturn(new PgPayment(new TransactionKey(transactionKey)));

                HttpEntity<PaymentRequest> paymentHttpEntity = new HttpEntity<>(paymentRequest, paymentHeaders);
                ParameterizedTypeReference<ApiResponse<PaymentResponse>> paymentResponseType =
                        new ParameterizedTypeReference<>() {
                        };

                testRestTemplate.exchange(paymentEndPoint, HttpMethod.POST, paymentHttpEntity, paymentResponseType);
            }

            @Test
            @DisplayName("Status 200 - 결제 실패 처리")
            void status200_failure() {
                // Given
                String endPoint = "/api/v1/payments/callback";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest request =
                        new com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest(
                                transactionKey,
                                orderId,
                                "CREDIT",
                                "1234567890123456",
                                50000L,
                                "FAILED",
                                "카드 거절됨"
                        );

                HttpEntity<com.loopers.application.api.payment.PaymentV1Dto.PgCallbackRequest> httpEntity =
                        new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // When
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }
    }
}
