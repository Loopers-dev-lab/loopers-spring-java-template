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
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
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
    private final OrderService orderService;
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
            OrderService orderService,
            DatabaseCleanUp databaseCleanUp,
            RedisCleanUp redisCleanUp
    ) {
        this.testRestTemplate = restTemplate;
        this.userFacade = userFacade;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.brandJpaRepository = brandJpaRepository;
        this.stockService = stockService;
        this.orderService = orderService;
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

            // 비동기 이벤트 처리 대기 후 최종 상태 검증
            try {
                waitForAsyncProcessing();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("비동기 처리 대기 중 인터럽트 발생: " + e.getMessage());
            }
            // 주문 상태 확인 (비동기 처리 후)
            // 최종 주문 상태 확인, 재고 차감 확인, 결제 처리 확인 등은 별도 테스트 메서드로 분리
        }

        @DisplayName("성공 케이스: 주문 생성 후 비동기 이벤트 처리 확인")
        @Test
        void createOrder_withValidRequest_verifiesAsyncEventProcessing() throws InterruptedException {
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

            // assert - API 응답 확인
            assertTrue(response.getStatusCode().is2xxSuccessful());

            // 비동기 이벤트 처리 대기 (재고 차감 → 쿠폰 처리 → 결제 처리 → 주문 확인)
            waitForAsyncProcessing(2000);

            // 생성된 주문 조회하여 성공 상태 확인 (재시도 로직 사용)
            Order confirmedOrder = waitForOrderToConfirm(validUserId, 5);
            
            assertNotNull(confirmedOrder, "확인된 주문을 찾을 수 없습니다");
            assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getOrderStatus(), 
                    "주문 상태는 CONFIRMED여야 함");
            
            // 재고가 차감되었는지 확인
            Stock stock = stockService.findByProductId(testProductId)
                    .orElseThrow(() -> new RuntimeException("Stock을 찾을 수 없습니다"));
            assertEquals(98L, stock.getQuantity(), "재고가 2개 차감되어야 함 (100 - 2 = 98)");
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

        @DisplayName("실패 케이스: 재고 부족 시 주문 생성 후 비동기로 실패 처리됨")
        @Test
        void createOrder_withInsufficientStock_OrderCreatedButFailsAsync() throws InterruptedException {
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

            // assert - 주문은 생성되지만 비동기로 실패 처리됨
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    String.format("주문 생성은 성공해야 함 (비동기 처리), 실제 상태 코드: %s", response.getStatusCode()));
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            
            // 비동기 처리 대기 (재고 차감 실패 → 주문 실패 처리)
            // 더 긴 대기 시간 필요: 재고 차감 실패 → StockEvents.ProcessingFailed 발행 → OrderEventListener 처리
            waitForAsyncProcessing(3000); // 3초로 증가 (여러 단계의 비동기 처리)
            
            // 생성된 주문 조회하여 실패 상태 확인 (재시도 로직 사용)
            Order failedOrder = waitForOrderToFail(validUserId, 5);
            
            assertNotNull(failedOrder, "실패한 주문을 찾을 수 없습니다");
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(), 
                    "주문 상태는 PAYMENT_FAILED여야 함");
            assertNotNull(failedOrder.getErrorMessage(), "에러 메시지가 설정되어야 함");
            assertTrue(failedOrder.getErrorMessage().contains("재고") ||
                            failedOrder.getErrorMessage().contains("부족") ||
                            failedOrder.getErrorMessage().contains("Stock"),
                    String.format("에러 메시지에 재고 관련 내용이 포함되어야 함, 실제 메시지: %s", failedOrder.getErrorMessage()));
        }

        @DisplayName("실패 케이스: 결제 실패 시 주문 생성 후 비동기로 실패 처리됨")
        @Test
        void createOrder_withPaymentFailure_OrderCreatedButFailsAsync() throws InterruptedException {
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

            // assert - 주문은 생성되지만 비동기로 실패 처리됨
            assertTrue(response.getStatusCode().is2xxSuccessful(),
                    String.format("주문 생성은 성공해야 함 (비동기 처리), 실제 상태 코드: %s", response.getStatusCode()));
            assertNotNull(response.getBody(),
                    "응답 본문은 null이 아니어야 함");
            
            // 비동기 처리 대기 (재고 차감 → 쿠폰 처리 → 결제 실패 → 주문 실패 처리)
            waitForAsyncProcessing(2000); // 더 긴 대기 시간 (결제 실패 → PaymentEvents.ProcessingFailed → OrderEventListener)
            
            // 생성된 주문 조회하여 실패 상태 확인 (재시도 로직 사용)
            Order failedOrder = waitForOrderToFail(validUserId, 5);
            
            assertNotNull(failedOrder, "실패한 주문을 찾을 수 없습니다");
            assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.getOrderStatus(), 
                    "주문 상태는 PAYMENT_FAILED여야 함");
            assertNotNull(failedOrder.getErrorMessage(), "에러 메시지가 설정되어야 함");
            assertTrue(failedOrder.getErrorMessage().contains("결제") ||
                            failedOrder.getErrorMessage().contains("결제 요청에 실패했습니다") ||
                            failedOrder.getErrorMessage().contains("Payment"),
                    String.format("에러 메시지에 결제 관련 내용이 포함되어야 함, 실제 메시지: %s", failedOrder.getErrorMessage()));
        }
    }

    /**
     * 비동기 이벤트 처리 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        waitForAsyncProcessing(1000);
    }
    
    /**
     * 비동기 이벤트 처리 대기 (지정된 시간)
     */
    private void waitForAsyncProcessing(long millis) throws InterruptedException {
        Thread.sleep(millis); // Saga 전체 흐름이므로 충분한 대기 시간 필요
    }
    
    /**
     * 주문이 확인 상태가 될 때까지 대기 (재시도 로직 포함)
     */
    private Order waitForOrderToConfirm(Long userId, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            List<Order> orders = orderService.findOrdersByUserId(userId);
            if (orders.isEmpty()) {
                Thread.sleep(200); // 200ms 대기 후 재시도
                continue;
            }
            
            Order confirmedOrder = orders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRMED)
                    .findFirst()
                    .orElse(null);
            
            if (confirmedOrder != null) {
                return confirmedOrder;
            }
            
            Thread.sleep(200); // 200ms 대기 후 재시도
        }
        
        // 마지막으로 한 번 더 확인
        List<Order> orders = orderService.findOrdersByUserId(userId);
        if (orders.isEmpty()) {
            throw new RuntimeException("주문이 생성되지 않았습니다");
        }
        
        Order confirmedOrder = orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.CONFIRMED)
                .findFirst()
                .orElse(null);
        
        if (confirmedOrder == null) {
            // 디버깅을 위해 모든 주문 상태 출력
            System.out.println("모든 주문 상태:");
            orders.forEach(order -> System.out.println(
                    String.format("Order ID: %d, Status: %s, ErrorMessage: %s", 
                            order.getId(), order.getOrderStatus(), order.getErrorMessage())));
            throw new RuntimeException("CONFIRMED 상태의 주문을 찾을 수 없습니다");
        }
        
        return confirmedOrder;
    }
    
    /**
     * 주문이 실패 상태가 될 때까지 대기 (재시도 로직 포함)
     */
    private Order waitForOrderToFail(Long userId, int maxRetries) throws InterruptedException {
        for (int i = 0; i < maxRetries; i++) {
            List<Order> orders = orderService.findOrdersByUserId(userId);
            if (orders.isEmpty()) {
                System.out.println(String.format("[재시도 %d/%d] 주문이 아직 생성되지 않음", i + 1, maxRetries));
                Thread.sleep(500); // 500ms 대기 후 재시도 (더 긴 대기 시간)
                continue;
            }
            
            Order failedOrder = orders.stream()
                    .filter(order -> order.getOrderStatus() == OrderStatus.PAYMENT_FAILED)
                    .findFirst()
                    .orElse(null);
            
            if (failedOrder != null) {
                System.out.println(String.format("주문 실패 상태 확인 성공 - Order ID: %d, Status: %s", 
                        failedOrder.getId(), failedOrder.getOrderStatus()));
                return failedOrder;
            }
            
            // 디버깅을 위해 모든 주문 상태 출력 (매번 출력하여 진행 상황 확인)
            System.out.println(String.format("[재시도 %d/%d] 모든 주문 상태:", i + 1, maxRetries));
            orders.forEach(order -> System.out.println(
                    String.format("  Order ID: %d, Status: %s, ErrorMessage: %s", 
                            order.getId(), order.getOrderStatus(), order.getErrorMessage())));
            
            Thread.sleep(500); // 500ms 대기 후 재시도 (더 긴 대기 시간)
        }
        
        // 마지막으로 한 번 더 확인
        List<Order> orders = orderService.findOrdersByUserId(userId);
        if (orders.isEmpty()) {
            throw new RuntimeException("주문이 생성되지 않았습니다");
        }
        
        Order failedOrder = orders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.PAYMENT_FAILED)
                .findFirst()
                .orElse(null);
        
        if (failedOrder == null) {
            // 디버깅을 위해 모든 주문 상태 출력
            System.out.println("최종 확인 - 모든 주문 상태:");
            orders.forEach(order -> System.out.println(
                    String.format("  Order ID: %d, Status: %s, ErrorMessage: %s", 
                            order.getId(), order.getOrderStatus(), order.getErrorMessage())));
            throw new RuntimeException("PAYMENT_FAILED 상태의 주문을 찾을 수 없습니다");
        }
        
        return failedOrder;
    }
}

