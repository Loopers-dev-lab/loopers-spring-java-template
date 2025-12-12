package com.loopers.interfaces.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaymentV1ApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PointRepository pointRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.create("testuser", "test@mail.com", "1990-01-01", Gender.MALE);
        userRepository.save(testUser);

        Point point = Point.create(testUser.getId(), 100000L);
        pointRepository.save(point);

        Brand brand = Brand.create("Test Brand");
        brandRepository.save(brand);

        testProduct = Product.create("Test Product", 10000L, 100, brand);
        productRepository.save(testProduct);
    }

    @Nested
    @DisplayName("POST /api/v1/payments/point - 포인트 결제")
    class PayWithPoint {

        @DisplayName("포인트 결제를 성공할 수 있다.")
        @Test
        void payWithPoint_success() throws Exception {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    orderInfo.orderId(),
                    0L,
                    null,
                    "point-key-" + System.currentTimeMillis()
            );

            // when & then
            mockMvc.perform(post("/api/v1/payments/point")
                            .header("X-USER-ID", testUser.getUserIdValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentId").exists())
                    .andExpect(jsonPath("$.data.orderId").value(orderInfo.orderId()))
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        }

        @DisplayName("멱등성 키가 없으면 400 에러가 발생한다.")
        @Test
        void payWithPoint_missingIdempotencyKey() throws Exception {
            // given
            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    1L,
                    0L,
                    null,
                    null // 멱등성 키 누락
            );

            // when & then
            mockMvc.perform(post("/api/v1/payments/point")
                            .header("X-USER-ID", testUser.getUserIdValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/payments/card - PG 카드 결제")
    class PayWithCard {

        @DisplayName("PG 카드 결제 요청을 생성할 수 있다.")
        @Test
        void payWithCard_request() throws Exception {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.PG_CARD,
                    new OrderPlaceCommand.CardInfo("SAMSUNG", "1234-5678-9012-3456")
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentV1Dto.CardPaymentRequest request = new PaymentV1Dto.CardPaymentRequest(
                    orderInfo.orderId(),
                    "SAMSUNG",
                    "1234-5678-9012-3456",
                    0L,
                    null,
                    "card-key-" + System.currentTimeMillis()
            );

            // when & then
            mockMvc.perform(post("/api/v1/payments/card")
                            .header("X-USER-ID", testUser.getUserIdValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentId").exists())
                    .andExpect(jsonPath("$.data.orderId").value(orderInfo.orderId()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @DisplayName("카드 정보가 없으면 400 에러가 발생한다.")
        @Test
        void payWithCard_missingCardInfo() throws Exception {
            // given
            PaymentV1Dto.CardPaymentRequest request = new PaymentV1Dto.CardPaymentRequest(
                    1L,
                    null, // 카드 타입 누락
                    null, // 카드 번호 누락
                    0L,
                    null,
                    "card-key-" + System.currentTimeMillis()
            );

            // when & then
            mockMvc.perform(post("/api/v1/payments/card")
                            .header("X-USER-ID", testUser.getUserIdValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId} - 결제 정보 조회")
    class GetPaymentDetail {

        @DisplayName("결제 정보를 조회할 수 있다.")
        @Test
        void getPaymentDetail() throws Exception {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    orderInfo.orderId(),
                    0L,
                    null,
                    "point-key-" + System.currentTimeMillis()
            );

            String response = mockMvc.perform(post("/api/v1/payments/point")
                            .header("X-USER-ID", testUser.getUserIdValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            Long paymentId = objectMapper.readTree(response).get("data").get("paymentId").asLong();

            // when & then
            mockMvc.perform(get("/api/v1/payments/" + paymentId)
                            .header("X-USER-ID", testUser.getUserIdValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.paymentId").value(paymentId))
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/orders/{orderId} - 주문별 결제 정보 조회")
    class GetPaymentByOrderId {

        @DisplayName("주문 ID로 결제 정보를 조회할 수 있다.")
        @Test
        void getPaymentByOrderId() throws Exception {
            // given
            OrderPlaceCommand orderCommand = new OrderPlaceCommand(
                    testUser.getUserIdValue(),
                    List.of(new OrderPlaceCommand.OrderItemCommand(testProduct.getId(), 1)),
                    null,
                    OrderPlaceCommand.PaymentMethod.POINT,
                    null
            );
            OrderInfo orderInfo = orderFacade.createOrder(orderCommand);

            PaymentV1Dto.PointPaymentRequest request = new PaymentV1Dto.PointPaymentRequest(
                    orderInfo.orderId(),
                    0L,
                    null,
                    "point-key-" + System.currentTimeMillis()
            );

            mockMvc.perform(post("/api/v1/payments/point")
                    .header("X-USER-ID", testUser.getUserIdValue())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // when & then
            mockMvc.perform(get("/api/v1/payments/orders/" + orderInfo.orderId())
                            .header("X-USER-ID", testUser.getUserIdValue()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(orderInfo.orderId()))
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        }
    }
}
