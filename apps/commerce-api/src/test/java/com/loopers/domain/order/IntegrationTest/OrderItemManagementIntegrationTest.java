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
public class OrderItemManagementIntegrationTest {
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

    @BeforeEach
    void setUp() {
        // 테스트 데이터는 각 테스트에서 필요한 만큼만 생성
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("주문 항목 관리")
    class OrderItemManagement {

        @Test
        @DisplayName("주문 요약 목록 조회 시 주문 항목 정보는 포함하지 않고 항목 개수만 포함한다")
        void should_not_include_order_items_when_retrieving_order_summary_list() {
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

            // Given: 여러 항목을 포함한 주문 생성
            orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(3).build()
                    ))
                    .build());

            // When: 주문 요약 목록 페이징 조회
            Page<OrderSummary> orderSummaries = orderFacade.getOrderSummariesByUserId(
                    userInfo.id(),
                    PageRequest.of(0, 10)
            );

            // Then: 주문 항목 정보는 포함하지 않고 항목 개수만 포함
            assertThat(orderSummaries).isNotNull();
            assertThat(orderSummaries.getContent()).hasSize(1);
            assertThat(orderSummaries.getTotalElements()).isEqualTo(1);
            assertThat(orderSummaries.getContent().get(0).itemCount()).isEqualTo(2);
            assertThat(orderSummaries.getContent().get(0).finalTotalAmount()).isEqualTo(new BigDecimal("80000.00"));
        }

        @Test
        @DisplayName("주문 ID로 주문 항목 목록을 페이징하여 조회할 수 있다")
        void should_retrieve_order_items_with_pagination() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("500000"));

            // Given: 10개의 상품 생성
            List<Long> productIds = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                ProductEntity product = productService.registerProduct(
                        ProductTestFixture.createRequest(
                                brand.getId(),
                                "상품" + i,
                                "설명" + i,
                                new BigDecimal("10000"),
                                100
                        )
                );
                productIds.add(product.getId());
            }

            // Given: 10개 항목을 포함한 주문 생성
            List<OrderItemCommand> itemCommands = productIds.stream()
                    .map(productId -> OrderItemCommand.builder()
                            .productId(productId)
                            .quantity(1)
                            .build())
                    .toList();

            OrderInfo createdOrder = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(itemCommands)
                    .build());

            // When: 주문 항목을 페이지 크기 3으로 첫 번째 페이지 조회
            Page<OrderItemInfo> firstPage =
                    orderFacade.getOrderItemsByOrderId(
                            createdOrder.id(),
                            userInfo.username(),
                            PageRequest.of(0, 3)
                    );

            // Then: 페이징 결과 검증
            assertThat(firstPage).isNotNull();
            assertThat(firstPage.getContent()).hasSize(3);
            assertThat(firstPage.getTotalElements()).isEqualTo(10);
            assertThat(firstPage.getTotalPages()).isEqualTo(4);
            assertThat(firstPage.getNumber()).isEqualTo(0);

            // When: 두 번째 페이지 조회
            Page<OrderItemInfo> secondPage =
                    orderFacade.getOrderItemsByOrderId(
                            createdOrder.id(),
                            userInfo.username(), PageRequest.of(1, 3)
                    );

            // Then: 두 번째 페이지 검증
            assertThat(secondPage.getContent()).hasSize(3);
            assertThat(secondPage.getNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("주문 상세 조회 시에만 주문 항목 전체를 조회한다")
        void should_retrieve_all_order_items_only_when_getting_order_detail() {
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
            OrderInfo createdOrder = orderFacade.createOrder(OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(1).build(),
                            OrderItemCommand.builder().productId(product3.getId()).quantity(3).build()
                    ))
                    .build());

            // When: 주문 상세 조회
            OrderInfo orderDetail = orderFacade.getOrderById(userInfo.username(), createdOrder.id());

            // Then: 주문 항목 전체가 조회됨
            assertThat(orderDetail.orderItems()).isNotNull();
            assertThat(orderDetail.orderItems()).hasSize(3);
            assertThat(orderDetail.orderItems())
                    .extracting("productId", "quantity")
                    .containsExactlyInAnyOrder(
                            tuple(product1.getId(), 2),
                            tuple(product2.getId(), 1),
                            tuple(product3.getId(), 3)
                    );
        }

        @Test
        @DisplayName("존재하지 않는 주문의 주문 항목 조회 시 예외가 발생한다")
        void should_throw_exception_when_retrieving_items_of_non_existent_order() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 99999L;
            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));


            // When & Then: 존재하지 않는 주문의 항목 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderItemsByOrderId(
                    nonExistentOrderId,
                    userInfo.username(), PageRequest.of(0, 10)
            ))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
    }
}
