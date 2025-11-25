package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.util.List;

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
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 11. 17.
 */
@SpringBootTest
public class OrderConfirmationIntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

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
    @DisplayName("주문 확정")
    class OrderConfirmation {

        @Test
        @DisplayName("PENDING 상태의 주문을 확정하면 CONFIRMED 상태로 변경된다")
        void should_change_status_to_confirmed_when_confirming_pending_order() {
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

            // When: 주문 확정
            OrderInfo confirmedOrder = orderFacade.confirmOrder(createdOrder.id(), userInfo.username());

            // Then: 주문 상태가 CONFIRMED로 변경되었는지 검증
            assertThat(confirmedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(confirmedOrder.id()).isEqualTo(createdOrder.id());
        }

        @Test
        @DisplayName("PENDING이 아닌 상태의 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_non_pending_order() {
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
            orderFacade.confirmOrder(createdOrder.id(), userInfo.username());  // 첫 번째 확정

            // When & Then: 이미 확정된 주문을 다시 확정하려고 하면 예외 발생
            assertThatThrownBy(() -> orderFacade.confirmOrder(createdOrder.id(), userInfo.username()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문 확정은 대기 상태 또는 활성화된 주문만 가능합니다.");
        }

        @Test
        @DisplayName("존재하지 않는 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_non_existent_order() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 99999L;

            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);

            // When & Then: 존재하지 않는 주문 확정 시 예외 발생
            assertThatThrownBy(() -> orderFacade.confirmOrder(nonExistentOrderId, userInfo.username()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("삭제된 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_deleted_order() {
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
                                    .quantity(1)
                                    .build()
                    ))
                    .build();
            OrderInfo createdOrder = orderFacade.createOrder(orderCommand);

            // Given: 주문 삭제 (소프트 삭제)
            orderService.deleteOrder(createdOrder.id(), userInfo.username());

            // When & Then: 삭제된 주문 확정 시 예외 발생 (삭제된 주문은 조회되지 않음)
            assertThatThrownBy(() -> orderFacade.confirmOrder(createdOrder.id(), userInfo.username()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
    }

}
