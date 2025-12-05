package com.loopers.interfaces.api;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PgFeignClient;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Order E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class OrderE2ETest {

    private static final String ENDPOINT = "/api/v1/orders";

    @MockitoBean
    private PgFeignClient pgFeignClient;

    private final TestRestTemplate testRestTemplate;
    private final UserFacade userFacade;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BrandJpaRepository brandJpaRepository;
    private final StockService stockService;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;

    @Autowired
    public OrderE2ETest(
            TestRestTemplate restTemplate,
            UserFacade userFacade,
            UserRepository userRepository,
            ProductRepository productRepository,
            BrandJpaRepository brandJpaRepository,
            StockService stockService,
            DatabaseCleanUp databaseCleanUp,
            RedisCleanUp redisCleanUp
    ) {
        this.testRestTemplate = restTemplate;
        this.userFacade = userFacade;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.stockService = stockService;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }

    private Long testProductId;
    private Long validUserId;

    private final String testLoginId = "test34";

    @BeforeEach
    void setUp() {
        // 테스트용 User 생성
        UserInfo userInfo = UserInfo.builder()
                .loginId(testLoginId)
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
        userFacade.saveUser(userInfo);
        
        // 생성된 User의 ID를 가져옴
        validUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Brand 생성
        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();
        Brand savedBrand = brandJpaRepository.save(brand);

        // 테스트용 Product 생성
        Product product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brandId(savedBrand.getId())
                .build();
        Product savedProduct = productRepository.save(product)
                .orElseThrow(() -> new RuntimeException("Product 저장 실패"));
        testProductId = savedProduct.getId();

        // Product 저장 후 Stock 별도 생성
        Stock stock = Stock.builder()
                .productId(savedProduct.getId())
                .quantity(0L)
                .build();
        stockService.saveStock(stock)
                .orElseThrow(() -> new RuntimeException("Stock 저장 실패"));

        // 테스트용 Stock 재고를 100개로 설정
        stockService.increaseQuantity(testProductId, 100L);

        // PgFeignClient Mock 기본 설정 (성공 응답)
        // PG API는 동기 응답으로 항상 PENDING을 반환하고, 이후 비동기 콜백으로 SUCCESS/FAILED 처리됨
        when(pgFeignClient.approvePayment(any(Long.class), any(PaymentDto.PgRequest.class)))
                .thenReturn(ApiResponse.success(PaymentDto.PgResponse.builder()
                        .transactionKey("20250101:TR:abc123")
                        .status(PaymentDto.PaymentStatus.PENDING)  // 동기 응답은 항상 PENDING
                        .reason(null)
                        .build()));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        // 멱등성 키는 10초 TTL이므로 테스트 간 정리 (이전 테스트의 멱등성 키가 남아있지 않도록)
        redisCleanUp.truncateAll();
    }

    @DisplayName("주문 생성 API")
    @Nested
    class CreateOrderApiTest {

        @DisplayName("성공 케이스: 올바른 HTTP 요청 시 200 OK 응답")
        @Test
        void createOrder_withValidRequest_Returns200Ok() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // Debug: 에러 발생 시 상세 정보 출력
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.out.println("=== Error Response Details ===");
                System.out.println("Status Code: " + response.getStatusCode());
                System.out.println("Response Body: " + response.getBody());
                if (response.getBody() != null && response.getBody().meta() != null) {
                    System.out.println("Error Message: " + response.getBody().meta().message());
                    System.out.println("Error Code: " + response.getBody().meta().errorCode());
                }
                System.out.println("testProductId: " + testProductId);
                System.out.println("validUserId: " + validUserId);
                System.out.println("==============================");
            }

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    String.format("예상 상태 코드: 2xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertEquals(ApiResponse.Metadata.Result.SUCCESS, response.getBody().meta().result(),
                    String.format("예상 결과: SUCCESS, 실제 결과: %s", response.getBody().meta().result()));
            assertNull(response.getBody().data(),
                    "응답 data는 null이어야 함");
            assertNull(response.getBody().meta().errorCode(),
                    "에러 코드는 null이어야 함");
            assertNull(response.getBody().meta().message(),
                    "에러 메시지는 null이어야 함");
        }

        @DisplayName("실패 케이스: X-USER-ID 헤더 누락 시 400 Bad Request 응답")
        @Test
        void createOrder_withoutXUserIdHeader_Returns400BadRequest() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertEquals(ApiResponse.Metadata.Result.FAIL, response.getBody().meta().result(),
                    String.format("예상 결과: FAIL, 실제 결과: %s", response.getBody().meta().result()));
        }

        @DisplayName("실패 케이스: X-USER-ID 헤더 형식 오류 시 400 Bad Request 응답")
        @Test
        void createOrder_withInvalidXUserIdHeader_Returns400BadRequest() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "invalid-number"); // Long이 아닌 값
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
        }

        @DisplayName("실패 케이스: Request Body 누락 시 400 Bad Request 응답")
        @Test
        void createOrder_withoutRequestBody_Returns400BadRequest() {
            // arrange
            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
        }

        @DisplayName("실패 케이스: 잘못된 JSON 형식 시 400 Bad Request 응답")
        @Test
        void createOrder_withInvalidJson_Returns400BadRequest() {
            // arrange
            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            String invalidJson = "{ invalid json }";
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(invalidJson, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
        }

        @DisplayName("실패 케이스: items 필드 누락 시 400 Bad Request 응답")
        @Test
        void createOrder_withMissingItemsField_Returns400BadRequest() {
            // arrange
            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestWithoutItems = "{\"couponIds\": []}";
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(requestWithoutItems, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertTrue(response.getBody().meta().message().equals("주문 항목이 1개 이상이어야 합니다."),
                    String.format("예상 메시지: '주문 항목이 1개 이상이어야 합니다.', 실제 메시지: %s", response.getBody().meta().message()));
        }

        @DisplayName("실패 케이스: 잘못된 필드 타입 시 400 Bad Request 응답")
        @Test
        void createOrder_withInvalidFieldType_Returns400BadRequest() {
            // arrange
            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestWithInvalidType = "{\"items\": [{\"productId\": \"invalid\", \"quantity\": 2}], \"couponIds\": []}";
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(requestWithInvalidType, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertTrue(response.getBody().meta().message().contains("타입") ||
                            response.getBody().meta().message().contains("productId"),
                    String.format("예상 메시지: '타입' 또는 'productId' 관련 오류 포함, 실제 메시지: %s", response.getBody().meta().message()));
        }

        @DisplayName("실패 케이스: 존재하지 않는 Product 시 404 Not Found 응답")
        @Test
        void createOrder_withNonExistentProduct_Returns404NotFound() {
            // arrange
            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(99999L) // 존재하지 않는 Product ID
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertEquals(ApiResponse.Metadata.Result.FAIL, response.getBody().meta().result(),
                    String.format("예상 결과: FAIL, 실제 결과: %s", response.getBody().meta().result()));
            assertTrue(response.getBody().meta().message().contains("Product를 찾을 수 없습니다"),
                    String.format("예상 메시지: 'Product를 찾을 수 없습니다' 포함, 실제 메시지: %s", response.getBody().meta().message()));
        }

        @DisplayName("실패 케이스: 재고 부족 시 400 Bad Request 응답")
        @Test
        void createOrder_withInsufficientStock_Returns400BadRequest() {
            // arrange
            // 재고를 5개로 설정
            stockService.decreaseQuantity(testProductId, 95L); // 100 - 95 = 5

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(10) // 재고보다 많은 수량
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError(),
                    String.format("예상 상태 코드: 4xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertEquals(ApiResponse.Metadata.Result.FAIL, response.getBody().meta().result(),
                    String.format("예상 결과: FAIL, 실제 결과: %s", response.getBody().meta().result()));
            assertTrue(response.getBody().meta().message().contains("재고") ||
                            response.getBody().meta().message().contains("부족"),
                    String.format("예상 메시지: '재고' 또는 '부족' 포함, 실제 메시지: %s", response.getBody().meta().message()));
        }

        @DisplayName("실패 케이스: 결제 실패 시 500 Internal Server Error 응답")
        @Test
        void createOrder_withPaymentFailure_Returns500InternalServerError() {
            // arrange
            // PgFeignClient Mock을 실패 응답으로 설정
            when(pgFeignClient.approvePayment(any(Long.class), any(PaymentDto.PgRequest.class)))
                    .thenReturn(ApiResponse.success(PaymentDto.PgResponse.builder()
                            .transactionKey("20250101:TR:fail123")
                            .status(PaymentDto.PaymentStatus.FAILED)
                            .reason("결제 요청에 실패했습니다")
                            .build()));

            List<OrderDto.OrderItemRequest> items = List.of(
                    OrderDto.OrderItemRequest.builder()
                            .productId(testProductId)
                            .quantity(2)
                            .build()
            );
            OrderDto.CreateOrderRequest request = OrderDto.CreateOrderRequest.builder()
                    .items(items)
                    .couponIds(new ArrayList<>())
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId.toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is5xxServerError(),
                    String.format("예상 상태 코드: 5xx, 실제 상태 코드: %s", response.getStatusCode()));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            assertEquals(ApiResponse.Metadata.Result.FAIL, response.getBody().meta().result(),
                    String.format("예상 결과: FAIL, 실제 결과: %s", response.getBody().meta().result()));
            assertTrue(response.getBody().meta().message().contains("결제 요청에 실패했습니다"),
                    String.format("예상 메시지: '결제 요청에 실패했습니다' 포함, 실제 메시지: %s", response.getBody().meta().message()));
        }
    }
}

