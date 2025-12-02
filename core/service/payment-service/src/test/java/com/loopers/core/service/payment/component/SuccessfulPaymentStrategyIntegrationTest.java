package com.loopers.core.service.payment.component;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

@DisplayName("결제 성공 전략 통합 테스트")
class SuccessfulPaymentStrategyIntegrationTest extends IntegrationTest {

    @Autowired
    private SuccessfulPaymentStrategy successfulPaymentStrategy;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        testUser = userRepository.save(
                Instancio.of(User.class)
                        .set(field(User::getId), UserId.empty())
                        .set(field(User::getIdentifier), UserIdentifier.create("testuser"))
                        .set(field(User::getEmail), new UserEmail("test@example.com"))
                        .create()
        );

        // 상품 생성 (가격: 10,000, 재고: 1,000개)
        testProduct = productRepository.save(
                Instancio.of(Product.class)
                        .set(field(Product::getId), ProductId.empty())
                        .set(field(Product::getBrandId), new BrandId("1"))
                        .set(field(Product::getName), new ProductName("테스트 상품"))
                        .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                        .set(field(Product::getStock), new ProductStock(1000L))
                        .set(field(Product::getLikeCount), ProductLikeCount.init())
                        .create()
        );

        // 주문 생성
        testOrder = orderRepository.save(
                Instancio.of(Order.class)
                        .set(field(Order::getId), OrderId.empty())
                        .set(field(Order::getUserId), testUser.getId())
                        .set(field(Order::getCreatedAt), CreatedAt.now())
                        .set(field(Order::getUpdatedAt), UpdatedAt.now())
                        .set(field(Order::getDeletedAt), DeletedAt.empty())
                        .create()
        );

        // 주문 상품 생성 (10개 구매)
        orderItemRepository.save(
                Instancio.of(OrderItem.class)
                        .set(field(OrderItem::getId), OrderItemId.empty())
                        .set(field(OrderItem::getOrderId), testOrder.getId())
                        .set(field(OrderItem::getProductId), testProduct.getId())
                        .set(field(OrderItem::getQuantity), new Quantity(10L))
                        .create()
        );
    }

    @Nested
    @DisplayName("handle 메서드 - 단순 재고 차감")
    class HandleMethod {

        @Test
        @DisplayName("결제 성공 시 주문 상품의 재고를 정확하게 차감한다")
        void shouldDecreaseStockCorrectly() {
            // Given
            long initialStock = testProduct.getStock().value();
            Payment payment = Payment.create(
                    testOrder.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );

            // When
            Payment result = successfulPaymentStrategy.pay(payment, new FailedReason(""));

            // Then
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 SUCCESS로 변경되어야 함")
                        .isEqualTo(PaymentStatus.SUCCESS);
                softly.assertThat(updatedProduct.getStock().value())
                        .as("재고가 정확하게 10개 차감되어야 함")
                        .isEqualTo(initialStock - 10);
            });
        }

        @Test
        @DisplayName("여러 상품의 재고를 모두 정확하게 차감한다")
        void shouldDecreaseStockForMultipleItems() {
            // Given
            Product product2 = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("2"))
                            .set(field(Product::getName), new ProductName("테스트 상품 2"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("20000")))
                            .set(field(Product::getStock), new ProductStock(500L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            // 두 번째 상품도 주문에 추가 (20개 구매)
            orderItemRepository.save(
                    Instancio.of(OrderItem.class)
                            .set(field(OrderItem::getId), OrderItemId.empty())
                            .set(field(OrderItem::getOrderId), testOrder.getId())
                            .set(field(OrderItem::getProductId), product2.getId())
                            .set(field(OrderItem::getQuantity), new Quantity(20L))
                            .create()
            );

            long product1InitialStock = testProduct.getStock().value();
            long product2InitialStock = product2.getStock().value();

            // When
            Payment payment = Payment.create(
                    testOrder.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );
            Payment result = successfulPaymentStrategy.pay(payment, new FailedReason(""));

            // Then
            Product updatedProduct1 = productRepository.findById(testProduct.getId()).orElseThrow();
            Product updatedProduct2 = productRepository.findById(product2.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 SUCCESS로 변경되어야 함")
                        .isEqualTo(PaymentStatus.SUCCESS);
                softly.assertThat(updatedProduct1.getStock().value())
                        .as("첫 번째 상품 재고가 10개 차감되어야 함")
                        .isEqualTo(product1InitialStock - 10);
                softly.assertThat(updatedProduct2.getStock().value())
                        .as("두 번째 상품 재고가 20개 차감되어야 함")
                        .isEqualTo(product2InitialStock - 20);
            });
        }
    }

    @Nested
    @DisplayName("동시성 제어 - 비관적 락 검증")
    class ConcurrencyControl {

        @Test
        @DisplayName("같은 상품에 대해 동시에 여러 결제가 발생할 때 재고 일관성을 보장한다")
        void shouldMaintainStockConsistencyUnderConcurrentPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentPaymentCount = 5;
            int stockPerPayment = 10;

            // 동시 결제를 위한 주문들 생성 및 보관
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();
            for (int i = 0; i < concurrentPaymentCount; i++) {
                Order order = orderRepository.save(
                        Instancio.of(Order.class)
                                .set(field(Order::getId), OrderId.empty())
                                .set(field(Order::getUserId), testUser.getId())
                                .set(field(Order::getCreatedAt), CreatedAt.now())
                                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                                .set(field(Order::getDeletedAt), DeletedAt.empty())
                                .create()
                );
                orderItemRepository.save(
                        Instancio.of(OrderItem.class)
                                .set(field(OrderItem::getId), OrderItemId.empty())
                                .set(field(OrderItem::getOrderId), order.getId())
                                .set(field(OrderItem::getProductId), testProduct.getId())
                                .set(field(OrderItem::getQuantity), new Quantity((long) stockPerPayment))
                                .create()
                );
                concurrentOrders.add(order);
            }

            // When - Virtual Thread를 사용한 병렬 결제 처리
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        successfulPaymentStrategy.pay(payment, new FailedReason(""));
                    }
            );

            // Then - 재고 일관성 검증
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock - (concurrentPaymentCount * stockPerPayment);

            assertThat(finalProduct.getStock().value())
                    .as("동시 결제로 인한 재고 차감이 정확하게 계산되어야 함 (초기: %d, 현재: %d, 예상: %d)",
                            initialStock, finalProduct.getStock().value(), expectedStock)
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("10개의 동시 결제에서 재고 일관성을 보장한다")
        void shouldMaintainStockConsistencyWith10ConcurrentPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentPaymentCount = 10;
            int stockPerPayment = 5;

            // 각 동시 결제를 위한 주문 준비 및 보관
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();
            for (int i = 0; i < concurrentPaymentCount; i++) {
                Order order = orderRepository.save(
                        Instancio.of(Order.class)
                                .set(field(Order::getId), OrderId.empty())
                                .set(field(Order::getUserId), testUser.getId())
                                .set(field(Order::getCreatedAt), CreatedAt.now())
                                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                                .set(field(Order::getDeletedAt), DeletedAt.empty())
                                .create()
                );
                orderItemRepository.save(
                        Instancio.of(OrderItem.class)
                                .set(field(OrderItem::getId), OrderItemId.empty())
                                .set(field(OrderItem::getOrderId), order.getId())
                                .set(field(OrderItem::getProductId), testProduct.getId())
                                .set(field(OrderItem::getQuantity), new Quantity((long) stockPerPayment))
                                .create()
                );
                concurrentOrders.add(order);
            }

            // When - 10개 스레드로 동시 결제 실행
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        successfulPaymentStrategy.pay(payment, new FailedReason(""));
                    }
            );

            // Then - 최종 재고 검증
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock - (concurrentPaymentCount * stockPerPayment);

            assertThat(finalProduct.getStock().value())
                    .as("10개의 동시 결제로 인한 재고 일관성 (초기: %d, 현재: %d, 예상: %d)",
                            initialStock, finalProduct.getStock().value(), expectedStock)
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("서로 다른 상품에 대한 동시 결제에서 각 상품의 재고가 독립적으로 관리된다")
        void shouldManageStockIndependentlyForDifferentProducts() throws InterruptedException {
            // Given
            Product product1 = testProduct;
            Product product2 = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("2"))
                            .set(field(Product::getName), new ProductName("테스트 상품 2"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                            .set(field(Product::getStock), new ProductStock(1000L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );
            Product product3 = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("3"))
                            .set(field(Product::getName), new ProductName("테스트 상품 3"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                            .set(field(Product::getStock), new ProductStock(1000L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            long product1InitialStock = product1.getStock().value();
            long product2InitialStock = product2.getStock().value();
            long product3InitialStock = product3.getStock().value();

            int concurrentPaymentCount = 9; // 3개 상품 × 3개 결제씩
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();

            // 각 상품별로 3개씩 주문 생성
            for (int i = 0; i < concurrentPaymentCount; i++) {
                Product targetProduct = switch (i / 3) {
                    case 0 -> product1;
                    case 1 -> product2;
                    default -> product3;
                };

                Order order = orderRepository.save(
                        Instancio.of(Order.class)
                                .set(field(Order::getId), OrderId.empty())
                                .set(field(Order::getUserId), testUser.getId())
                                .set(field(Order::getCreatedAt), CreatedAt.now())
                                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                                .set(field(Order::getDeletedAt), DeletedAt.empty())
                                .create()
                );
                orderItemRepository.save(
                        Instancio.of(OrderItem.class)
                                .set(field(OrderItem::getId), OrderItemId.empty())
                                .set(field(OrderItem::getOrderId), order.getId())
                                .set(field(OrderItem::getProductId), targetProduct.getId())
                                .set(field(OrderItem::getQuantity), new Quantity(10L))
                                .create()
                );
                concurrentOrders.add(order);
            }

            // When - 각 상품별로 3개씩 동시 결제
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        successfulPaymentStrategy.pay(payment, new FailedReason(""));
                    }
            );

            // Then - 각 상품의 재고가 독립적으로 정확하게 차감되었는지 검증
            Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            Product finalProduct3 = productRepository.findById(product3.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(finalProduct1.getStock().value())
                        .as("상품1: 3개 결제로 30개 차감 (초기 %d → 현재 %d)",
                                product1InitialStock, finalProduct1.getStock().value())
                        .isEqualTo(product1InitialStock - 30);
                softly.assertThat(finalProduct2.getStock().value())
                        .as("상품2: 3개 결제로 30개 차감 (초기 %d → 현재 %d)",
                                product2InitialStock, finalProduct2.getStock().value())
                        .isEqualTo(product2InitialStock - 30);
                softly.assertThat(finalProduct3.getStock().value())
                        .as("상품3: 3개 결제로 30개 차감 (초기 %d → 현재 %d)",
                                product3InitialStock, finalProduct3.getStock().value())
                        .isEqualTo(product3InitialStock - 30);
            });
        }
    }

    @Nested
    @DisplayName("재고 부족 시 결제 실패 처리")
    class StockShortageHandling {

        @Test
        @DisplayName("재고가 부족하면 결제를 FAILED 상태로 반환한다")
        void shouldFailPaymentWhenStockIsInsufficient() {
            // Given
            Product productWithLowStock = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("2"))
                            .set(field(Product::getName), new ProductName("재고 부족 상품"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                            .set(field(Product::getStock), new ProductStock(5L)) // 재고 5개
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            Order order = orderRepository.save(
                    Instancio.of(Order.class)
                            .set(field(Order::getId), OrderId.empty())
                            .set(field(Order::getUserId), testUser.getId())
                            .set(field(Order::getCreatedAt), CreatedAt.now())
                            .set(field(Order::getUpdatedAt), UpdatedAt.now())
                            .set(field(Order::getDeletedAt), DeletedAt.empty())
                            .create()
            );

            // 10개 주문 (재고는 5개)
            orderItemRepository.save(
                    Instancio.of(OrderItem.class)
                            .set(field(OrderItem::getId), OrderItemId.empty())
                            .set(field(OrderItem::getOrderId), order.getId())
                            .set(field(OrderItem::getProductId), productWithLowStock.getId())
                            .set(field(OrderItem::getQuantity), new Quantity(10L))
                            .create()
            );

            Payment payment = Payment.create(
                    order.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );

            // When
            Payment result = successfulPaymentStrategy.pay(payment, new FailedReason(""));

            // Then
            Product unchangedProduct = productRepository.findById(productWithLowStock.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 FAILED로 변경되어야 함")
                        .isEqualTo(PaymentStatus.FAILED);
                softly.assertThat(result.getFailedReason().value())
                        .as("실패 원인이 재고 부족 메시지여야 함")
                        .isEqualTo("상품의 재고가 부족합니다.");
                softly.assertThat(unchangedProduct.getStock().value())
                        .as("재고는 변경되지 않아야 함")
                        .isEqualTo(5L);
            });
        }

        @Test
        @Transactional
        @DisplayName("여러 상품 중 하나의 재고가 부족하면 결제를 FAILED 상태로 반환하고 모든 재고 변경을 롤백한다")
        void shouldFailPaymentAndRollbackAllStockChangesWhenOneProductIsOutOfStock() {
            // Given
            // 상품1: 재고 충분 (100개)
            Product product1 = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("1"))
                            .set(field(Product::getName), new ProductName("재고 충분 상품"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                            .set(field(Product::getStock), new ProductStock(100L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            // 상품2: 재고 부족 (5개)
            Product product2 = productRepository.save(
                    Instancio.of(Product.class)
                            .set(field(Product::getId), ProductId.empty())
                            .set(field(Product::getBrandId), new BrandId("2"))
                            .set(field(Product::getName), new ProductName("재고 부족 상품"))
                            .set(field(Product::getPrice), new ProductPrice(new BigDecimal("20000")))
                            .set(field(Product::getStock), new ProductStock(5L))
                            .set(field(Product::getLikeCount), ProductLikeCount.init())
                            .create()
            );

            Order order = orderRepository.save(
                    Instancio.of(Order.class)
                            .set(field(Order::getId), OrderId.empty())
                            .set(field(Order::getUserId), testUser.getId())
                            .set(field(Order::getCreatedAt), CreatedAt.now())
                            .set(field(Order::getUpdatedAt), UpdatedAt.now())
                            .set(field(Order::getDeletedAt), DeletedAt.empty())
                            .create()
            );

            // 상품1 10개 주문 (성공 가능)
            orderItemRepository.save(
                    Instancio.of(OrderItem.class)
                            .set(field(OrderItem::getId), OrderItemId.empty())
                            .set(field(OrderItem::getOrderId), order.getId())
                            .set(field(OrderItem::getProductId), product1.getId())
                            .set(field(OrderItem::getQuantity), new Quantity(10L))
                            .create()
            );

            // 상품2 10개 주문 (실패 예정 - 재고 5개)
            orderItemRepository.save(
                    Instancio.of(OrderItem.class)
                            .set(field(OrderItem::getId), OrderItemId.empty())
                            .set(field(OrderItem::getOrderId), order.getId())
                            .set(field(OrderItem::getProductId), product2.getId())
                            .set(field(OrderItem::getQuantity), new Quantity(10L))
                            .create()
            );

            Payment payment = Payment.create(
                    order.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );

            long product1InitialStock = product1.getStock().value();
            long product2InitialStock = product2.getStock().value();

            // When
            Payment result = successfulPaymentStrategy.pay(payment, new FailedReason(""));

            // Then
            Product product1AfterPayment = productRepository.findById(product1.getId()).orElseThrow();
            Product product2AfterPayment = productRepository.findById(product2.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 FAILED로 변경되어야 함")
                        .isEqualTo(PaymentStatus.FAILED);
                softly.assertThat(result.getFailedReason().value())
                        .as("실패 원인이 재고 부족 메시지여야 함")
                        .isEqualTo("상품의 재고가 부족합니다.");
                softly.assertThat(product1AfterPayment.getStock().value())
                        .as("상품1의 재고는 변경되지 않아야 함 (모든 검증 후 저장)")
                        .isEqualTo(product1InitialStock);
                softly.assertThat(product2AfterPayment.getStock().value())
                        .as("상품2의 재고는 변경되지 않아야 함")
                        .isEqualTo(product2InitialStock);
            });
        }
    }
}
