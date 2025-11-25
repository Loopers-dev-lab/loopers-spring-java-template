package com.loopers.domain.order.IntegrationTest;

import static com.loopers.domain.point.PointTransactionType.CHARGE;
import static com.loopers.domain.point.PointTransactionType.REFUND;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loopers.application.order.OrderCreateCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderItemCommand;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 17.
 */
@SpringBootTest
public class OrderCancelIntegrationTest {
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
    @DisplayName("주문 취소 및 재고 원복 테스트")
    class order_cancellation_and_stock_restorationTest {

        @Test
        @DisplayName("주문 취소 시 재고가 원복되고 포인트가 환불된다")
        void 주문_취소_시_재고가_원복되고_포인트가_환불된다() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            Integer initialStock = 100;
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    initialStock
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 주문 생성
            Integer orderQuantity = 2;
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(orderQuantity)
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // 주문 생성 후 재고 확인
            ProductEntity productAfterOrder = productService.getProductDetail(product.getId());
            assertThat(productAfterOrder.getStockQuantity()).isEqualTo(initialStock - orderQuantity);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 재고 원복 확인
            ProductEntity productAfterCancel = productService.getProductDetail(product.getId());
            assertThat(productAfterCancel.getStockQuantity()).isEqualTo(initialStock);

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);

            // Then: 포인트 환불 확인
            PointHistoryEntity pointHistoryEntity = pointService.getPointHistories(userInfo.username()).get(0);

            assertThat(pointHistoryEntity.getTransactionType()).isEqualTo(REFUND);
            assertThat(pointHistoryEntity.getAmount()).isEqualTo(new BigDecimal("20000").setScale(2));

            assertThat(pointHistoryEntity.getBalanceAfter())
                    .isEqualTo(new BigDecimal("50000").setScale(2));
        }

        @Test
        @DisplayName("PENDING 상태의 주문을 취소할 수 있다")
        void PENDING_상태의_주문을_취소할_수_있다() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 주문 생성
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);
            assertThat(createdOrder.status()).isEqualTo(OrderStatus.PENDING);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("CONFIRMED 상태의 주문을 취소할 수 있다")
        void CONFIRMED_상태의_주문을_취소할_수_있다() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity product = productService.registerProduct(productRequest);

            // Given: 주문 생성 및 확정
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(1)
                                    .build()
                    ))
                    .build();

            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);
            OrderInfo confirmedOrder = orderFacade.confirmOrder(createdOrder.id() , userInfo.username());
            assertThat(confirmedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(confirmedOrder.id(), userInfo.username());

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
