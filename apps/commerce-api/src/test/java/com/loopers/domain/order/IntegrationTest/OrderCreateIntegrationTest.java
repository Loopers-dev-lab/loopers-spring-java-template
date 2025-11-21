package com.loopers.domain.order.IntegrationTest;

import static org.assertj.core.api.Assertions.*;
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
import com.loopers.domain.product.ProductDomainCreateRequest;
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
public class OrderCreateIntegrationTest {
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
    @DisplayName("주문 생성")
    class OrderCreation {

        @Test
        @DisplayName("유효한 사용자와 상품으로 주문을 생성하면 성공한다")
        void should_create_order_successfully_with_valid_user_and_products() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명"));

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand command = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(command);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성
            ProductDomainCreateRequest productRequest = ProductTestFixture.createRequest(
                    brand.getId(),
                    "테스트상품",
                    "상품 설명",
                    new BigDecimal("10000"),
                    100
            );
            ProductEntity savedProduct = productService.registerProduct(productRequest);

            // Given: 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(savedProduct.getId())
                                    .quantity(2)
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문이 정상 생성되었는지 검증
            assertThat(result).isNotNull();
            assertThat(result.userId()).isEqualTo(userInfo.id());
            assertThat(result.originalTotalAmount()).isEqualTo(new BigDecimal("20000.00")); // 10000 * 2
            assertThat(result.finalTotalAmount()).isEqualTo(new BigDecimal("20000.00")); // 할인 없음
            assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(result.orderItems()).hasSize(1);
            assertThat(result.orderItems().get(0).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("주문 생성 시 주문 항목들이 함께 저장된다")
        void should_save_order_items_when_creating_order() {
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
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품1",
                            "상품1 설명",
                            new BigDecimal("10000"),
                            100
                    )
            );

            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품2",
                            "상품2 설명",
                            new BigDecimal("20000"),
                            50
                    )
            );

            ProductEntity product3 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품3",
                            "상품3 설명",
                            new BigDecimal("15000"),
                            30
                    )
            );

            // Given: 여러 상품을 포함한 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product1.getId())
                                    .quantity(2)
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product2.getId())
                                    .quantity(1)
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product3.getId())
                                    .quantity(3)
                                    .build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문 항목들이 모두 저장되었는지 검증
            assertThat(result.orderItems()).isNotNull();
            assertThat(result.orderItems()).hasSize(3);

            // Then: 각 주문 항목의 정보가 정확한지 검증
            assertThat(result.orderItems())
                    .extracting("productId", "quantity", "unitPrice")
                    .containsExactlyInAnyOrder(
                            tuple(product1.getId(), 2, new BigDecimal("10000.00")),
                            tuple(product2.getId(), 1, new BigDecimal("20000.00")),
                            tuple(product3.getId(), 3, new BigDecimal("15000.00"))
                    );

            // Then: 각 주문 항목의 총액이 정확한지 검증
            assertThat(result.orderItems().get(0).totalPrice())
                    .isEqualTo(result.orderItems().get(0).unitPrice()
                            .multiply(BigDecimal.valueOf(result.orderItems().get(0).quantity())));

            // Then: 주문 총액이 모든 항목의 합계와 일치하는지 검증
            BigDecimal expectedTotal = new BigDecimal("10000.00").multiply(BigDecimal.valueOf(2))  // 20000
                    .add(new BigDecimal("20000.00").multiply(BigDecimal.valueOf(1)))                    // 20000
                    .add(new BigDecimal("15000.00").multiply(BigDecimal.valueOf(3)));                   // 45000
            assertThat(result.finalTotalAmount()).isEqualTo(expectedTotal);  // 85000.00
        }

        @Test
        @DisplayName("주문 생성 시 총액이 주문 항목들의 합계와 일치한다")
        void should_match_total_amount_with_sum_of_order_items() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("200000"));

            // Given: 다양한 가격의 상품 생성
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품1", "설명1", new BigDecimal("12500"), 100)
            );
            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품2", "설명2", new BigDecimal("33000"), 50)
            );
            ProductEntity product3 = productService.registerProduct(
                    ProductTestFixture.createRequest(brand.getId(), "상품3", "설명3", new BigDecimal("7800"), 30)
            );

            // Given: 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(3).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product3.getId()).quantity(5).build()
                    ))
                    .build();

            // When: 주문 생성
            OrderInfo result = orderFacade.createOrder(orderCommand);

            // Then: 주문 항목들의 총액 합계 계산
            BigDecimal itemsTotal = result.orderItems().stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Then: 주문 총액과 항목 합계가 일치하는지 검증
            assertThat(result.finalTotalAmount()).isEqualTo(itemsTotal);

            // Then: 예상 총액과도 일치하는지 검증
            BigDecimal expectedTotal = new BigDecimal("12500.00").multiply(BigDecimal.valueOf(3))  // 37500
                    .add(new BigDecimal("33000.00").multiply(BigDecimal.valueOf(2)))                    // 66000
                    .add(new BigDecimal("7800.00").multiply(BigDecimal.valueOf(5)));                    // 39000
            assertThat(result.finalTotalAmount()).isEqualTo(expectedTotal);  // 142500.00
        }

        @Test
        @DisplayName("재고가 부족한 상품으로 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_insufficient_stock() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 재고가 적은 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "재고부족상품",
                            "재고가 5개만 있는 상품",
                            new BigDecimal("10000"),
                            5  // 재고 5개
                    )
            );

            // Given: 재고보다 많은 수량으로 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(10)  // 재고(5개)보다 많은 수량 요청
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 시 예외 발생
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 상품입니다.")
                    .hasMessageContaining("요청 수량: 10")
                    .hasMessageContaining("재고: 5");
        }

        @Test
        @DisplayName("포인트가 부족한 경우 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_insufficient_points() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 적은 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("5000"));  // 5,000원만 충전

            // Given: 상품 생성
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "고가상품",
                            "비싼 상품",
                            new BigDecimal("10000"),
                            100
                    )
            );

            // Given: 포인트보다 비싼 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(2)  // 20,000원 (보유 포인트 5,000원보다 많음)
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 시 포인트 부족 예외 발생
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트가 부족합니다");
        }

        @Test
        @DisplayName("삭제된 상품으로 주문 생성 시 예외가 발생한다")
        void should_throw_exception_when_creating_order_with_deleted_product() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("50000"));

            // Given: 상품 생성 후 삭제
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "삭제될상품",
                            "곧 삭제될 상품",
                            new BigDecimal("10000"),
                            100
                    )
            );
            Long productId = product.getId();
            product.delete();
            productRepository.save(product);


            // Given: 삭제된 상품으로 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(productId)
                                    .quantity(1)
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 시 예외 발생
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }
    }
}
