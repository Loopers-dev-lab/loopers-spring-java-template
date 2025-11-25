package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.interfaces.api.order.OrderV1Dtos;
import com.loopers.interfaces.api.point.PointV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Order API E2E 테스트")
class OrderV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PointFacade pointFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    public OrderV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    private String testUsername;
    private Long testProductId;

    @BeforeEach
    void setUp() {
        // 테스트용 브랜드 생성
        BrandEntity brand = brandService.registerBrand(
                BrandTestFixture.createRequest("테스트브랜드", "E2E 테스트용 브랜드")
        );

        // 테스트용 상품 생성
        ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                brand.getId(),
                "테스트상품",
                "E2E 테스트용 상품",
                new BigDecimal("10000"),
                1000
        );
        ProductEntity product = productService.registerProduct(productRequest);
        testProductId = product.getId();

        // 테스트용 사용자 생성
        UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
        userFacade.registerUser(userCommand);
        testUsername = userCommand.username();

        // 테스트용 포인트 충전 (충분한 금액)
        PointV1Dtos.PointChargeRequest chargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000000"));
        pointFacade.chargePoint(testUsername, chargeRequest);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        testProductId = null;
        testUsername = null;
    }

    @Nested
    @DisplayName("주문 등록 API")
    class CreateOrderTest {

        @Test
        @DisplayName("올바른 주문 정보로 주문을 등록하면 주문 정보를 응답한다")
        void create_order_success() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.OrderCreateRequest request = new OrderV1Dtos.OrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 2, null))
            );

            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> response =
                    testRestTemplate.exchange(Uris.Order.CREATE, HttpMethod.POST,
                            new HttpEntity<>(request, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().orderId()).isNotNull(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().status())
                            .isEqualTo(OrderStatus.PENDING),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().originalTotalAmount())
                            .isEqualByComparingTo(new BigDecimal("20000.00")),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().finalTotalAmount())
                            .isEqualByComparingTo(new BigDecimal("20000.00"))
            );
        }

        @Test
        @DisplayName("X-USER-ID 헤더가 없으면 400 Bad Request 응답을 반환한다")
        void create_order_fail_when_header_missing() {
            // given
            OrderV1Dtos.OrderCreateRequest request = new OrderV1Dtos.OrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null))
            );

            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> response =
                    testRestTemplate.exchange(Uris.Order.CREATE, HttpMethod.POST,
                            new HttpEntity<>(request, null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 주문 등록 시 404 Not Found 응답을 반환한다")
        void create_order_fail_when_user_not_found() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistentuser");
            headers.setContentType(MediaType.APPLICATION_JSON);

            OrderV1Dtos.OrderCreateRequest request = new OrderV1Dtos.OrderCreateRequest(
                    List.of(new OrderV1Dtos.OrderItemRequest(testProductId, 1, null))
            );

            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderCreateResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderCreateResponse>> response =
                    testRestTemplate.exchange(Uris.Order.CREATE, HttpMethod.POST,
                            new HttpEntity<>(request, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @Nested
    @DisplayName("주문 목록 조회 API")
    class GetOrdersTest {

        @Test
        @DisplayName("사용자의 주문 목록을 페이징하여 조회한다")
        void get_orders_with_pagination_success() {
            // given
            createTestOrder(testUsername, 1);
            createTestOrder(testUsername, 2);
            createTestOrder(testUsername, 3);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> response =
                    testRestTemplate.exchange(Uris.Order.GET_LIST + "?page=0&size=2",
                            HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).hasSize(2),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalElements())
                            .isEqualTo(3),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalPages())
                            .isEqualTo(2)
            );
        }

        @Test
        @DisplayName("주문이 없는 사용자는 빈 목록을 응답받는다")
        void get_orders_returns_empty_list_when_no_orders() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> response =
                    testRestTemplate.exchange(Uris.Order.GET_LIST,
                            HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().content()).isEmpty(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalElements())
                            .isEqualTo(0)
            );
        }

        @Test
        @DisplayName("X-USER-ID 헤더가 없으면 400 Bad Request 응답을 반환한다")
        void get_orders_fail_when_header_missing() {
            // when
            ParameterizedTypeReference<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PageResponse<OrderV1Dtos.OrderListResponse>>> response =
                    testRestTemplate.exchange(Uris.Order.GET_LIST,
                            HttpMethod.GET, new HttpEntity<>(null, null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 API")
    class GetOrderDetailTest {

        @Test
        @DisplayName("주문 ID로 주문 상세 정보를 조회한다")
        void get_order_detail_success() {
            // given
            Long orderId = createTestOrder(testUsername, 3);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Order.GET_DETAIL,
                            HttpMethod.GET,  new HttpEntity<>(null, headers), responseType, orderId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().orderId())
                            .isEqualTo(orderId),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().userId()).isNotNull(),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().originalTotalAmount())
                            .isEqualByComparingTo(new BigDecimal("30000.00")),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().finalTotalAmount())
                            .isEqualByComparingTo(new BigDecimal("30000.00")),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().items())
                            .isNotEmpty()
            );
        }

        @Test
        @DisplayName("존재하지 않는 주문 ID로 조회하면 404 Not Found 응답을 반환한다")
        void get_order_detail_fail_when_order_not_found() {
            // given

            Long nonExistentOrderId = 99999L;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", testUsername);

            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Order.GET_DETAIL,
                            HttpMethod.GET,  new HttpEntity<>(null, headers), responseType, nonExistentOrderId);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @Test
        @DisplayName("숫자가 아닌 주문 ID로 조회하면 400 Bad Request 응답을 반환한다")
        void get_order_detail_fail_when_invalid_order_id() {
            // when
            ParameterizedTypeReference<ApiResponse<OrderV1Dtos.OrderDetailResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<OrderV1Dtos.OrderDetailResponse>> response =
                    testRestTemplate.exchange(Uris.Order.BASE + "/invalid",
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    // 테스트 헬퍼 메서드
    private Long createTestOrder(String username, int quantity) {
        OrderCreateCommand command = new OrderCreateCommand(
                username,
                List.of(new OrderItemCommand(testProductId, quantity, null))
        );
        OrderInfo orderInfo = orderFacade.createOrder(command);
        return orderInfo.id();
    }
}
