package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.loopers.application.order.*;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 17.
 */
@SpringBootTest
public class OrderRetrievalIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;


    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private PointService pointService;
    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("주문 조회")
    class OrderRetrieval {

        @Test
        @DisplayName("주문 ID로 주문 요약 정보를 조회할 수 있다")
        void should_retrieve_order_summary_by_id() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "테스트상품",
                            "상품 설명",
                            new BigDecimal("10000"),
                            100
                    )
            );

            // Given: 주문 생성
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)
                                    .build()
                    ))
                    .build();
            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // When: 주문 ID로 요약 정보 조회
            OrderSummary retrievedOrder = orderFacade.getOrderSummaryById(createdOrder.id(), userInfo.username());

            // Then: 주문 요약 정보가 정확히 조회되는지 검증
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.id()).isEqualTo(createdOrder.id());
            assertThat(retrievedOrder.userId()).isEqualTo(userInfo.id());
            assertThat(retrievedOrder.finalTotalAmount()).isEqualTo(new BigDecimal("20000.00"));
            assertThat(retrievedOrder.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(retrievedOrder.itemCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("사용자 ID로 주문 요약 목록을 페이징하여 조회할 수 있다")
        void should_retrieve_order_summaries_by_user_id_with_pagination() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 상품 생성
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품1", "설명1", new BigDecimal("10000"), 100)
            );
            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품2", "설명2", new BigDecimal("20000"), 100)
            );

            // Given: 여러 개의 주문 생성
            OrderInfo order1 = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product1.getId()).quantity(1).build()))
                    .build());

            OrderInfo order2 = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product2.getId()).quantity(2).build()))
                    .build());

            // When: 사용자 ID로 주문 요약 목록 페이징 조회
            Page<OrderSummary> orderPage = orderFacade.getOrderSummariesByUserId(
                    userInfo.id(),
                    PageRequest.of(0, 10)
            );

            // Then: 해당 사용자의 모든 주문이 조회되는지 검증
            assertThat(orderPage).isNotNull();
            assertThat(orderPage.getContent()).hasSize(2);
            assertThat(orderPage.getTotalElements()).isEqualTo(2);
            assertThat(orderPage.getContent()).extracting("id").containsExactlyInAnyOrder(order1.id(), order2.id());
            assertThat(orderPage.getContent()).allMatch(order -> order.userId().equals(userInfo.id()));
        }

        @Test
        @DisplayName("주문 조회 시 주문 항목들이 함께 조회된다")
        void should_retrieve_order_with_order_items() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 여러 상품 생성
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품1", "설명1", new BigDecimal("10000"), 100)
            );
            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품2", "설명2", new BigDecimal("20000"), 100)
            );
            ProductEntity product3 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품3", "설명3", new BigDecimal("15000"), 100)
            );

            // Given: 여러 항목을 포함한 주문 생성
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(1).build(),
                            OrderItemCommand.builder().productId(product3.getId()).quantity(3).build()
                    ))
                    .build();
            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // When: 주문 조회
            OrderInfo retrievedOrder = orderFacade.getOrderById(userInfo.username(), createdOrder.id());

            // Then: 주문 항목들이 함께 조회되는지 검증
            assertThat(retrievedOrder.orderItems()).isNotNull();
            assertThat(retrievedOrder.orderItems()).hasSize(3);
            assertThat(retrievedOrder.orderItems())
                    .extracting("productId", "quantity")
                    .containsExactlyInAnyOrder(
                            tuple(product1.getId(), 2),
                            tuple(product2.getId(), 1),
                            tuple(product3.getId(), 3)
                    );
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        void should_throw_exception_when_retrieving_non_existent_order() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 99999L;

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // When & Then: 존재하지 않는 주문 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderSummaryById(nonExistentOrderId, userInfo.username()))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("삭제된 주문은 조회되지 않는다")
        void should_not_retrieve_deleted_orders() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "테스트상품", "설명", new BigDecimal("10000"), 100)
            );

            // Given: 주문 생성
            OrderInfo createdOrder = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product.getId()).quantity(1).build()))
                    .build());

            // Given: 주문 삭제
            orderService.deleteOrder(createdOrder.id(), userInfo.username());

            // When & Then: 삭제된 주문 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderSummaryById(createdOrder.id(), userInfo.username()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("사용자의 주문 요약 목록을 페이징하여 조회할 수 있다")
        void should_retrieve_user_order_summaries_with_pagination() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("500000"));

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "테스트상품", "설명", new BigDecimal("10000"), 1000)
            );

            // Given: 5개의 주문 생성
            for (int i = 0; i < 5; i++) {
                orderFacade.createOrder(OrderCreateCommand.builder()
                        .username(userInfo.username())
                        .orderItems(List.of(OrderItemCommand.builder().productId(product.getId()).quantity(1).build()))
                        .build());
            }

            // When: 페이지 크기 2로 첫 번째 페이지 조회
            Page<OrderSummary> firstPage = orderFacade.getOrderSummariesByUserId(
                    userInfo.id(),
                    PageRequest.of(0, 2)
            );

            // Then: 페이징 결과 검증
            assertThat(firstPage).isNotNull();
            assertThat(firstPage.getContent()).hasSize(2);
            assertThat(firstPage.getTotalElements()).isEqualTo(5);
            assertThat(firstPage.getTotalPages()).isEqualTo(3);
            assertThat(firstPage.getNumber()).isEqualTo(0);
            assertThat(firstPage.getContent()).allMatch(summary -> summary.itemCount() == 1);
        }

        @Test
        @DisplayName("주문 상태별로 필터링하여 페이징 조회할 수 있다")
        void should_filter_orders_by_status_with_pagination() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "테스트상품", "설명", new BigDecimal("10000"), 100)
            );

            // Given: 여러 주문 생성 (일부는 확정)
            OrderInfo order1 = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product.getId()).quantity(1).build()))
                    .build());

            OrderInfo order2 = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product.getId()).quantity(1).build()))
                    .build());

            OrderInfo order3 = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(OrderItemCommand.builder().productId(product.getId()).quantity(1).build()))
                    .build());

            // Given: 일부 주문 확정
            orderFacade.confirmOrder(order1.id(), userInfo.username());

            // When: CONFIRMED 상태의 주문만 페이징 조회
            Page<OrderSummary> confirmedOrders = orderFacade.getOrderSummariesByUserIdAndStatus(
                    userInfo.id(),
                    OrderStatus.CONFIRMED,
                    PageRequest.of(0, 10)
            );

            // Then: CONFIRMED 상태의 주문만 조회되는지 검증
            assertThat(confirmedOrders).isNotNull();
            assertThat(confirmedOrders.getContent()).hasSize(1);
            assertThat(confirmedOrders.getTotalElements()).isEqualTo(1);
            assertThat(confirmedOrders.getContent()).allMatch(order -> order.status() == OrderStatus.CONFIRMED);
            assertThat(confirmedOrders.getContent()).extracting("id").containsExactlyInAnyOrder(order1.id());

            // When: PENDING 상태의 주문만 페이징 조회
            Page<OrderSummary> pendingOrders = orderFacade.getOrderSummariesByUserIdAndStatus(
                    userInfo.id(),
                    OrderStatus.PENDING,
                    PageRequest.of(0, 10)
            );

            // Then: PENDING 상태의 주문만 조회되는지 검증
            assertThat(pendingOrders).isNotNull();
            assertThat(pendingOrders.getContent()).hasSize(2);
            assertThat(pendingOrders.getTotalElements()).isEqualTo(2);
            assertThat(pendingOrders.getContent().get(0).id()).isEqualTo(order2.id());
            assertThat(pendingOrders.getContent().get(0).status()).isEqualTo(OrderStatus.PENDING);
        }
    }
}
