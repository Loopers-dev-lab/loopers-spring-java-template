package com.loopers.interfaces.api;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.infrastructure.order.OrderJpaRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/orders/" + id;
    private static final String ENDPOINT_POST = "/api/v1/orders";

    private final TestRestTemplate testRestTemplate;
    private final OrderJpaRepository orderJpaRepository; // OrderJpaRepository 주입
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public OrderV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        OrderJpaRepository orderJpaRepository, // OrderJpaRepository 주입
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.orderJpaRepository = orderJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/orders/{id}")
    @Nested
    class Get {
        @DisplayName("존재하는 주문 ID를 주면, 해당 주문 정보를 반환한다.")
        @Test
        void returnsOrderInfo_whenValidIdIsProvided() {
            // arrange
            Long userId = 1L;
            Order order = Order.create(userId);
            order.getItems().add(OrderItem.create(1L, 2, Money.of(1000L), Money.of(1000L).multiply(2)));
            orderJpaRepository.save(order);

            String requestUrl = ENDPOINT_GET.apply(order.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(userId));

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo(order.getId()),
                () -> assertThat(response.getBody().data().item().size()).isEqualTo(order.getItems().size())
            );
        }

        @DisplayName("존재하지 않는 주문 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        void throwsException_whenInvalidIdIsProvided() {
            // arrange
            Long invalidId = 1L;
            String requestUrl = ENDPOINT_GET.apply(invalidId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(invalidId));

            // act
            ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
//
//    @DisplayName("POST /api/v1/orders")
//    @Nested
//    class Create {
//        @DisplayName("새로운 주문을 생성하면, 201 Created 응답을 반환하고 주문 정보를 반환한다.")
//        @Test
//        void returnsCreatedOrderInfo_whenNewOrderIsCreated() {
//            // arrange
//            Long userId = 1L;
//            List<OrderV1Dto.OrderItemInfo> items = new ArrayList<>();
//            items.add(new OrderV1Dto.OrderItemInfo(1L, 2, Money.of(1000L))); // 상품 ID, 수량, 가격
//
//            PaymentV1Dto.CardPaymentInfo cardPaymentInfo = new PaymentV1Dto.CardPaymentInfo(
//                "SAMSUNG", "1234-5678-1234-5678"
//            );
//
//            OrderV1Dto.OrderCreateRequest requestBody = new OrderV1Dto.OrderCreateRequest(
//                userId, items, cardPaymentInfo
//            );
//
//            // act
//            ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
//                testRestTemplate.postForEntity(ENDPOINT_POST, requestBody, new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {});
//
//            // assert
//            assertAll(
//                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
//                () -> assertThat(response.getBody().data().id()).isNotNull(),
//                () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
//                () -> assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.PENDING.name()) // 주문 생성 후 초기 상태는 PENDING
//            );
//        }
//    }
}
