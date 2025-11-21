package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.loopers.domain.product.ProductDomainCreateRequest;
import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.fixtures.ProductTestFixture;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;

/**
 * Order 도메인 통합 테스트
 *
 * <p>주문 생성, 확정, 취소 등의 전체 플로우를 검증합니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@SpringBootTest
@DisplayName("Order 도메인 통합 테스트")
public class OrderIntegrationTest {

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
            assertThat(result.totalAmount()).isEqualTo(new BigDecimal("20000.00")); // 10000 * 2
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
            assertThat(result.totalAmount()).isEqualTo(expectedTotal);  // 85000.00
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
            assertThat(result.totalAmount()).isEqualTo(itemsTotal);

            // Then: 예상 총액과도 일치하는지 검증
            BigDecimal expectedTotal = new BigDecimal("12500.00").multiply(BigDecimal.valueOf(3))  // 37500
                    .add(new BigDecimal("33000.00").multiply(BigDecimal.valueOf(2)))                    // 66000
                    .add(new BigDecimal("7800.00").multiply(BigDecimal.valueOf(5)));                    // 39000
            assertThat(result.totalAmount()).isEqualTo(expectedTotal);  // 142500.00
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
            OrderInfo confirmedOrder = orderFacade.confirmOrder(createdOrder.id());

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
            orderFacade.confirmOrder(createdOrder.id());  // 첫 번째 확정

            // When & Then: 이미 확정된 주문을 다시 확정하려고 하면 예외 발생
            assertThatThrownBy(() -> orderFacade.confirmOrder(createdOrder.id()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문 확정은 대기 상태 또는 활성화된 주문만 가능합니다. ");
        }

        @Test
        @DisplayName("존재하지 않는 주문 확정 시 예외가 발생한다")
        void should_throw_exception_when_confirming_non_existent_order() {
            // Given: 존재하지 않는 주문 ID
            Long nonExistentOrderId = 99999L;

            // When & Then: 존재하지 않는 주문 확정 시 예외 발생
            assertThatThrownBy(() -> orderFacade.confirmOrder(nonExistentOrderId))
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
            orderService.deleteOrder(createdOrder.id());

            // When & Then: 삭제된 주문 확정 시 예외 발생 (삭제된 주문은 조회되지 않음)
            assertThatThrownBy(() -> orderFacade.confirmOrder(createdOrder.id()))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
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
            OrderSummary retrievedOrder = orderFacade.getOrderSummaryById(createdOrder.id());

            // Then: 주문 요약 정보가 정확히 조회되는지 검증
            assertThat(retrievedOrder).isNotNull();
            assertThat(retrievedOrder.id()).isEqualTo(createdOrder.id());
            assertThat(retrievedOrder.userId()).isEqualTo(userInfo.id());
            assertThat(retrievedOrder.totalAmount()).isEqualTo(new BigDecimal("20000.00"));
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
            OrderInfo retrievedOrder = orderFacade.getOrderById(createdOrder.id());

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

            // When & Then: 존재하지 않는 주문 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderSummaryById(nonExistentOrderId))
                    .isInstanceOf(Exception.class)
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
            orderService.deleteOrder(createdOrder.id());

            // When & Then: 삭제된 주문 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderSummaryById(createdOrder.id()))
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
            orderFacade.confirmOrder(order1.id());
            orderFacade.confirmOrder(order2.id());

            // When: CONFIRMED 상태의 주문만 페이징 조회
            Page<OrderSummary> confirmedOrders = orderFacade.getOrderSummariesByUserIdAndStatus(
                    userInfo.id(),
                    OrderStatus.CONFIRMED,
                    PageRequest.of(0, 10)
            );

            // Then: CONFIRMED 상태의 주문만 조회되는지 검증
            assertThat(confirmedOrders).isNotNull();
            assertThat(confirmedOrders.getContent()).hasSize(2);
            assertThat(confirmedOrders.getTotalElements()).isEqualTo(2);
            assertThat(confirmedOrders.getContent()).allMatch(order -> order.status() == OrderStatus.CONFIRMED);
            assertThat(confirmedOrders.getContent()).extracting("id").containsExactlyInAnyOrder(order1.id(), order2.id());

            // When: PENDING 상태의 주문만 페이징 조회
            Page<OrderSummary> pendingOrders = orderFacade.getOrderSummariesByUserIdAndStatus(
                    userInfo.id(),
                    OrderStatus.PENDING,
                    PageRequest.of(0, 10)
            );

            // Then: PENDING 상태의 주문만 조회되는지 검증
            assertThat(pendingOrders).isNotNull();
            assertThat(pendingOrders.getContent()).hasSize(1);
            assertThat(pendingOrders.getTotalElements()).isEqualTo(1);
            assertThat(pendingOrders.getContent().get(0).id()).isEqualTo(order3.id());
            assertThat(pendingOrders.getContent().get(0).status()).isEqualTo(OrderStatus.PENDING);
        }
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
            assertThat(orderSummaries.getContent().get(0).totalAmount()).isEqualTo(new BigDecimal("80000.00"));
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
                            PageRequest.of(1, 3)
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
            OrderInfo orderDetail = orderFacade.getOrderById(createdOrder.id());

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

            // When & Then: 존재하지 않는 주문의 항목 조회 시 예외 발생
            assertThatThrownBy(() -> orderFacade.getOrderItemsByOrderId(
                    nonExistentOrderId,
                    PageRequest.of(0, 10)
            ))
                    .isInstanceOf(Exception.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("트랜잭션 및 동시성")
    class TransactionAndConcurrency {

        @Test
        @DisplayName("재고 예약 실패 시 이미 예약된 재고가 해제된다")
        void should_release_reserved_stock_when_subsequent_reservation_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("100000"));

            // Given: 두 개의 상품 생성 (하나는 재고 부족)
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "정상상품",
                            "재고가 충분한 상품",
                            new BigDecimal("10000"),
                            100
                    )
            );

            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "재고부족상품",
                            "재고가 부족한 상품",
                            new BigDecimal("20000"),
                            5  // 재고 5개
                    )
            );

            // Given: 첫 번째 상품은 정상, 두 번째 상품은 재고 부족으로 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product1.getId())
                                    .quantity(10)  // 정상 주문
                                    .build(),
                            OrderItemCommand.builder()
                                    .productId(product2.getId())
                                    .quantity(10)  // 재고 부족 (5개만 있음)
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문할 수 없는 상품입니다.");

            // Then: 첫 번째 상품의 재고가 원래대로 복구되었는지 확인
            ProductEntity product1AfterFail = productService.getProductDetail(product1.getId());
            assertThat(product1AfterFail.getStockQuantity()).isEqualTo(100);

            // Then: 두 번째 상품의 재고도 변경되지 않았는지 확인
            ProductEntity product2AfterFail = productService.getProductDetail(product2.getId());
            assertThat(product2AfterFail.getStockQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("포인트 차감 실패 시 예약된 모든 재고가 해제된다")
        void should_release_all_reserved_stock_when_point_deduction_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 부족한 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            pointService.charge(userInfo.username(), new BigDecimal("5000"));  // 부족한 포인트

            // Given: 여러 상품 생성
            ProductEntity product1 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품1",
                            "설명1",
                            new BigDecimal("10000"),
                            100
                    )
            );

            ProductEntity product2 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품2",
                            "설명2",
                            new BigDecimal("20000"),
                            50
                    )
            );

            ProductEntity product3 = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "상품3",
                            "설명3",
                            new BigDecimal("15000"),
                            30
                    )
            );

            // Given: 포인트가 부족한 주문 생성 요청
            OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder().productId(product1.getId()).quantity(2).build(),
                            OrderItemCommand.builder().productId(product2.getId()).quantity(1).build(),
                            OrderItemCommand.builder().productId(product3.getId()).quantity(3).build()
                    ))
                    .build();

            // When & Then: 포인트 부족으로 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(orderCommand))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            // Then: 모든 상품의 재고가 원래대로 복구되었는지 확인
            ProductEntity product1AfterFail = productService.getProductDetail(product1.getId());
            assertThat(product1AfterFail.getStockQuantity()).isEqualTo(100);

            ProductEntity product2AfterFail = productService.getProductDetail(product2.getId());
            assertThat(product2AfterFail.getStockQuantity()).isEqualTo(50);

            ProductEntity product3AfterFail = productService.getProductDetail(product3.getId());
            assertThat(product3AfterFail.getStockQuantity()).isEqualTo(30);

            // Then: 사용자 포인트도 차감되지 않았는지 확인
            UserEntity user = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(user.getPointAmount()).isEqualTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("주문 생성 실패 시 포인트 복구 및 재고 해제가 수행된다")
        void should_restore_points_and_release_stock_when_order_creation_fails() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("100000.00");
            pointService.charge(userInfo.username(), initialPoints);

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

            Integer initialStock = product.getStockQuantity();

            // Given: 유효하지 않은 주문 생성 요청 (수량이 0)
            OrderCreateCommand invalidOrderCommand = OrderCreateCommand.builder()
                    .username(userInfo.username())
                    .orderItems(List.of(
                            OrderItemCommand.builder()
                                    .productId(product.getId())
                                    .quantity(0)  // 유효하지 않은 수량
                                    .build()
                    ))
                    .build();

            // When & Then: 주문 생성 실패
            assertThatThrownBy(() -> orderFacade.createOrder(invalidOrderCommand))
                    .isInstanceOf(Exception.class);

            // Then: 재고가 원래대로 복구되었는지 확인
            ProductEntity productAfterFail = productService.getProductDetail(product.getId());
            assertThat(productAfterFail.getStockQuantity()).isEqualTo(initialStock);

            // Then: 포인트가 차감되지 않았는지 확인
            UserEntity user = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(user.getPointAmount()).isEqualTo(initialPoints);
        }

        @Test
        @DisplayName("동시에 같은 상품을 주문할 때 재고 차감이 정확하게 발생한다")
        void should_deduct_stock_accurately_when_ordering_same_product_concurrently() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 여러 사용자 생성 및 포인트 충전
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("50000"));
                users.add(userInfo);
            }

            // Given: 재고가 충분한 상품 생성
            Integer initialStock = 50;
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "인기상품",
                            "동시성 테스트용 상품",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 동시 주문 설정
            int threadCount = 10;
            int orderQuantityPerUser = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // When: 10명의 사용자가 동시에 각각 5개씩 주문 시도
            for (int i = 0; i < threadCount; i++) {
                final int userIndex = i;
                executorService.submit(() -> {
                    try {
                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(users.get(userIndex).username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(orderQuantityPerUser)
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then: 모든 스레드 완료 대기
            latch.await(30, TimeUnit.SECONDS);
            executorService.shutdown();

            // Then: 재고가 충분하므로 모든 주문이 성공해야 함
            assertThat(successCount.get()).isEqualTo(threadCount);
            assertThat(failCount.get()).isZero();

            // Then: 최종 재고가 정확히 차감되었는지 검증 (동시성 제어 완벽)
            ProductEntity finalProduct = productService.getProductDetail(product.getId());
            int expectedFinalStock = initialStock - (threadCount * orderQuantityPerUser);

            // ✅ 엄격한 검증: 정확히 일치해야 함
            assertThat(finalProduct.getStockQuantity())
                    .as("비관적 락으로 동시성이 보호되므로 재고는 정확히 일치해야 함")
                    .isEqualTo(expectedFinalStock);

            // ✅ 추가 검증: Oversell 절대 발생하지 않음
            assertThat(finalProduct.getStockQuantity())
                    .as("재고는 절대 음수가 될 수 없음")
                    .isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("동시에 같은 상품을 주문할 때 재고 부족 시 일부만 성공한다")
        void should_succeed_partially_when_stock_insufficient_during_concurrent_orders() throws InterruptedException {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 여러 사용자 생성 및 포인트 충전
            List<UserInfo> users = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UserRegisterCommand userCommand = new UserRegisterCommand(
                        "user" + i,
                        "user" + i + "@test.com",
                        LocalDate.of(1990, 1, 1).toString(),
                        Gender.MALE
                );
                UserInfo userInfo = userFacade.registerUser(userCommand);
                pointService.charge(userInfo.username(), new BigDecimal("100000"));
                users.add(userInfo);
            }

            // Given: 재고가 부족한 상품 생성
            Integer initialStock = 25;  // 10명이 5개씩 주문하면 부족 (50개 필요)
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "한정상품",
                            "재고가 부족한 한정 상품",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 동시 주문 설정
            int threadCount = 10;
            int orderQuantityPerUser = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // When: 10명의 사용자가 동시에 각각 5개씩 주문 시도
            for (int i = 0; i < threadCount; i++) {
                final int userIndex = i;
                executorService.submit(() -> {
                    try {
                        OrderCreateCommand orderCommand = OrderCreateCommand.builder()
                                .username(users.get(userIndex).username())
                                .orderItems(List.of(
                                        OrderItemCommand.builder()
                                                .productId(product.getId())
                                                .quantity(orderQuantityPerUser)
                                                .build()
                                ))
                                .build();

                        orderFacade.createOrder(orderCommand);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then: 모든 스레드 완료 대기
            latch.await(30, TimeUnit.SECONDS);
            executorService.shutdown();

            // Then: 일부 주문만 성공했는지 확인 (재고 부족으로 모두 성공할 수 없음)
            assertThat(successCount.get()).isLessThan(threadCount);
            assertThat(failCount.get()).isGreaterThan(0);

            // Then: 성공 + 실패 = 전체 시도 수
            assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);

            // Then: 최종 재고 확인 (재고가 소진되었거나 거의 소진됨)
            ProductEntity finalProduct = productService.getProductDetail(product.getId());
            assertThat(finalProduct.getStockQuantity()).isLessThanOrEqualTo(initialStock);

            // Then: 최소한 일부 재고는 차감되었는지 확인
            assertThat(finalProduct.getStockQuantity()).isLessThan(initialStock);
        }

        @Test
        @DisplayName("주문 취소 시 트랜잭션이 롤백되면 재고와 포인트가 원상태로 유지된다")
        void should_maintain_stock_and_points_when_cancel_transaction_rollback() {
            // Given: 브랜드 생성
            BrandEntity brand = brandService.registerBrand(
                    BrandTestFixture.createRequest("테스트브랜드", "브랜드 설명")
            );

            // Given: 사용자 생성 및 포인트 충전
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            UserInfo userInfo = userFacade.registerUser(userCommand);
            BigDecimal initialPoints = new BigDecimal("50000.00");
            pointService.charge(userInfo.username(), initialPoints);

            // Given: 상품 생성
            Integer initialStock = 100;
            ProductEntity product = productService.registerProduct(
                    ProductTestFixture.createRequest(
                            brand.getId(),
                            "테스트상품",
                            "상품 설명",
                            new BigDecimal("10000"),
                            initialStock
                    )
            );

            // Given: 주문 생성
            Integer orderQuantity = 5;
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

            // Given: 주문 생성 후 상태 확인
            ProductEntity productAfterOrder = productService.getProductDetail(product.getId());
            assertThat(productAfterOrder.getStockQuantity()).isEqualTo(initialStock - orderQuantity);

            UserEntity userAfterOrder = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            BigDecimal pointsAfterOrder = userAfterOrder.getPointAmount();
            assertThat(pointsAfterOrder).isLessThan(initialPoints);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(createdOrder.id(), userInfo.username());

            // Then: 재고 원복 확인
            ProductEntity productAfterCancel = productService.getProductDetail(product.getId());
            assertThat(productAfterCancel.getStockQuantity()).isEqualTo(initialStock);

            // Then: 포인트 환불 확인
            UserEntity userAfterCancel = userRepository.findByUsername(userInfo.username())
                    .orElseThrow();
            assertThat(userAfterCancel.getPointAmount()).isEqualTo(initialPoints);

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("주문 취소 및 재고 원복 테스트")
    class 주문_취소_및_재고_원복_테스트 {

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
            OrderInfo confirmedOrder = orderFacade.confirmOrder(createdOrder.id());
            assertThat(confirmedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);

            // When: 주문 취소
            OrderInfo cancelledOrder = orderFacade.cancelOrder(confirmedOrder.id(), userInfo.username());

            // Then: 주문 상태 확인
            assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
