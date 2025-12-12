package com.loopers.interfaces.api;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.CommercePayment;
import com.loopers.domain.payment.PaymentDto;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.payment.PaymentApiDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Payment E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentE2ETest {

    private static final String ENDPOINT = "/api/v1/payments/callback";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long testUserId;
    private Long testOrderId;
    private String testTransactionKey;

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

        testUserId = userRepository.findByLoginId(testLoginId)
                .orElseThrow(() -> new RuntimeException("User를 찾을 수 없습니다"))
                .getId();

        // 테스트용 Order 생성
        Order testOrder = Order.builder()
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .userId(testUserId)
                .build();
        Order savedOrder = orderService.saveOrder(testOrder);
        testOrderId = savedOrder.getId();

        // 테스트용 CommercePayment 생성
        testTransactionKey = "TEST_TRANSACTION_" + System.currentTimeMillis();
        CommercePayment payment = CommercePayment.builder()
                .transactionKey(testTransactionKey)
                .method(PaymentDto.PaymentMethod.CARD)
                .cardType(PaymentDto.CardType.SAMSUNG)
                .cardNo("1111-2222-3333-4444")
                .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                .orderId(testOrderId)
                .amount(BigDecimal.valueOf(10000))
                .build();
        paymentService.saveCommercePayment(payment);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("PG 콜백 API 테스트")
    @Nested
    class PaymentCallbackApiTest {

        @DisplayName("성공 케이스: PG 콜백 성공 시 200 OK 응답 및 결제 상태 변경")
        @Test
        void callbackPayment_withSuccessStatus_Returns200OkAndUpdatesPaymentStatus() throws InterruptedException {
            // arrange
            PaymentApiDto.PgCallbackRequest request = new PaymentApiDto.PgCallbackRequest(
                    testTransactionKey,
                    String.valueOf(testOrderId), // orderId
                    PaymentDto.CardType.SAMSUNG, // cardType
                    "1111-2222-3333-4444", // cardNo
                    10000L, // amount
                    PaymentDto.PaymentStatus.SUCCESS,
                    null // reason
            );

            String requestUrl = ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<PaymentApiDto.PaymentResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PaymentApiDto.PaymentResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(ApiResponse.Metadata.Result.SUCCESS, response.getBody().meta().result());

            // 비동기 이벤트 처리 대기
            waitForAsyncProcessing();

            // CommercePayment 상태 확인
            CommercePayment payment = paymentService.findByTransactionKey(testTransactionKey);
            assertEquals(PaymentDto.PaymentStatus.SUCCESS, payment.getPaymentStatus(),
                    "결제 상태가 SUCCESS로 변경되어야 함");

            // 주문 상태 확인 (비동기 처리 후)
            Order order = orderService.findOrderById(testOrderId);
            // PG 콜백 성공 시 주문이 CONFIRMED로 변경되는지 확인
            // 실제 구현에 따라 검증 로직 추가 필요
            assertNotNull(order, "주문이 존재해야 함");
        }

        @DisplayName("실패 케이스: PG 콜백 실패 시 결제 상태 FAILED로 변경")
        @Test
        void callbackPayment_withFailedStatus_UpdatesPaymentStatusToFailed() throws InterruptedException {
            // arrange
            PaymentApiDto.PgCallbackRequest request = new PaymentApiDto.PgCallbackRequest(
                    testTransactionKey,
                    String.valueOf(testOrderId), // orderId
                    PaymentDto.CardType.SAMSUNG, // cardType
                    "1111-2222-3333-4444", // cardNo
                    10000L, // amount
                    PaymentDto.PaymentStatus.FAILED,
                    "결제 요청에 실패했습니다"
            );

            String requestUrl = ENDPOINT;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ParameterizedTypeReference<ApiResponse<PaymentApiDto.PaymentResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PaymentApiDto.PaymentResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());

            // 비동기 이벤트 처리 대기
            waitForAsyncProcessing();

            // CommercePayment 상태 확인
            CommercePayment payment = paymentService.findByTransactionKey(testTransactionKey);
            assertEquals(PaymentDto.PaymentStatus.FAILED, payment.getPaymentStatus(),
                    "결제 상태가 FAILED로 변경되어야 함");

            // 주문 상태 확인 (비동기 처리 후)
            Order order = orderService.findOrderById(testOrderId);
            // PG 콜백 실패 시 주문이 PAYMENT_FAILED로 변경되는지 확인
            // 실제 구현에 따라 검증 로직 추가 필요
            assertNotNull(order, "주문이 존재해야 함");
        }
    }

    /**
     * 비동기 이벤트 처리 대기
     */
    private void waitForAsyncProcessing() throws InterruptedException {
        Thread.sleep(1000); // 비동기 이벤트 핸들러 완료 대기
    }
}
