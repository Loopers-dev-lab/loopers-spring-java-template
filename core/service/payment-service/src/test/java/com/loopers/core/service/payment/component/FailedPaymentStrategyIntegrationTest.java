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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

@DisplayName("결제 실패 전략 통합 테스트")
class FailedPaymentStrategyIntegrationTest extends IntegrationTest {

    @Autowired
    private FailedPaymentStrategy failedPaymentStrategy;

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
    @DisplayName("pay 메서드 - 결제 실패 시 재고 복구")
    class PayMethod {

        @Test
        @DisplayName("결제 실패 시 차감된 재고를 다시 증가시킨다")
        void shouldIncreaseStockWhenPaymentFails() {
            // Given
            long initialStock = testProduct.getStock().value();
            FailedReason failedReason = new FailedReason("결제 게이트웨이 오류");

            // 먼저 재고를 차감한 상태로 시뮬레이션 (실제로는 성공한 결제 후 실패)
            Product decreasedProduct = testProduct.decreaseStock(new Quantity(10L));
            productRepository.save(decreasedProduct);

            Payment payment = Payment.create(
                    testOrder.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );

            // When
            Payment result = failedPaymentStrategy.pay(payment, failedReason);

            // Then
            Product restoredProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 FAILED로 변경되어야 함")
                        .isEqualTo(PaymentStatus.FAILED);
                softly.assertThat(result.getFailedReason().value())
                        .as("실패 이유가 저장되어야 함")
                        .isEqualTo("결제 게이트웨이 오류");
                softly.assertThat(restoredProduct.getStock().value())
                        .as("재고가 원래 상태로 복구되어야 함")
                        .isEqualTo(initialStock);
            });
        }

        @Test
        @DisplayName("여러 상품의 재고를 모두 정확하게 복구한다")
        void shouldRestoreStockForMultipleItems() {
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

            // 둘 다 재고 차감 상태로 시뮬레이션
            Product decreasedProduct1 = testProduct.decreaseStock(new Quantity(10L));
            Product decreasedProduct2 = product2.decreaseStock(new Quantity(20L));
            productRepository.saveAll(java.util.List.of(decreasedProduct1, decreasedProduct2));

            Payment payment = Payment.create(
                    testOrder.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );
            FailedReason failedReason = new FailedReason("결제 처리 중 오류 발생");

            // When
            Payment result = failedPaymentStrategy.pay(payment, failedReason);

            // Then
            Product restoredProduct1 = productRepository.findById(testProduct.getId()).orElseThrow();
            Product restoredProduct2 = productRepository.findById(product2.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(result.getStatus())
                        .as("결제 상태가 FAILED로 변경되어야 함")
                        .isEqualTo(PaymentStatus.FAILED);
                softly.assertThat(restoredProduct1.getStock().value())
                        .as("첫 번째 상품 재고가 원래대로 복구되어야 함")
                        .isEqualTo(product1InitialStock);
                softly.assertThat(restoredProduct2.getStock().value())
                        .as("두 번째 상품 재고가 원래대로 복구되어야 함")
                        .isEqualTo(product2InitialStock);
            });
        }

        @Test
        @DisplayName("결제 실패 시 올바른 실패 이유를 반환한다")
        void shouldReturnCorrectFailedReasonInPayment() {
            // Given
            String expectedReason = "신용카드 한도 초과";
            FailedReason failedReason = new FailedReason(expectedReason);

            Product decreasedProduct = testProduct.decreaseStock(new Quantity(10L));
            productRepository.save(decreasedProduct);

            Payment payment = Payment.create(
                    testOrder.getOrderKey(),
                    testUser.getId(),
                    null,
                    null,
                    null
            );

            // When
            Payment result = failedPaymentStrategy.pay(payment, failedReason);

            // Then
            assertThat(result.getFailedReason().value())
                    .as("결제 객체에 올바른 실패 이유가 저장되어야 함")
                    .isEqualTo(expectedReason);
        }
    }

    @Nested
    @DisplayName("동시성 제어 - 동시 결제 실패 시 재고 복구 검증")
    class ConcurrencyControl {

        @Test
        @DisplayName("같은 상품에 대해 동시에 여러 결제가 실패할 때 재고 복구 일관성을 보장한다")
        void shouldMaintainStockConsistencyUnderConcurrentFailedPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentFailedPaymentCount = 5;
            int stockPerPayment = 10;

            // 동시 결제 실패를 위한 주문들 생성
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
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

            // 총 재고 차감 (5개 * 10 = 50)
            Product decreasedProduct = testProduct.decreaseStock(new Quantity((long) (concurrentFailedPaymentCount * stockPerPayment)));
            productRepository.save(decreasedProduct);

            // When - Virtual Thread를 사용한 병렬 결제 실패 처리
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        FailedReason failedReason = new FailedReason("결제 게이트웨이 오류 #" + index);
                        failedPaymentStrategy.pay(payment, failedReason);
                    }
            );

            // Then - 재고 일관성 검증 (모두 복구되어야 함)
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().value())
                    .as("동시 결제 실패로 인한 재고 복구가 정확하게 이루어져야 함 (초기: %d, 현재: %d)",
                            initialStock, finalProduct.getStock().value())
                    .isEqualTo(initialStock);
        }

        @Test
        @DisplayName("10개의 동시 결제 실패에서 재고 복구 일관성을 보장한다")
        void shouldMaintainStockConsistencyWith10ConcurrentFailedPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentFailedPaymentCount = 10;
            int stockPerPayment = 5;

            // 각 동시 결제 실패를 위한 주문 준비
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
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

            // 총 재고 차감 (10개 * 5 = 50)
            Product decreasedProduct = testProduct.decreaseStock(new Quantity((long) (concurrentFailedPaymentCount * stockPerPayment)));
            productRepository.save(decreasedProduct);

            // When - 10개 스레드로 동시 결제 실패 처리
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        FailedReason failedReason = new FailedReason("결제 실패: " + index);
                        failedPaymentStrategy.pay(payment, failedReason);
                    }
            );

            // Then - 최종 재고 검증
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().value())
                    .as("10개의 동시 결제 실패로 인한 재고 복구 일관성 (초기: %d, 현재: %d)",
                            initialStock, finalProduct.getStock().value())
                    .isEqualTo(initialStock);
        }

        @Test
        @DisplayName("서로 다른 상품에 대한 동시 결제 실패에서 각 상품의 재고가 독립적으로 복구된다")
        void shouldRestoreStockIndependentlyForDifferentProducts() throws InterruptedException {
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

            int concurrentFailedPaymentCount = 9; // 3개 상품 × 3개 결제씩
            java.util.List<Order> concurrentOrders = new java.util.ArrayList<>();

            // 각 상품별로 3개씩 주문 생성
            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
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

            // 각 상품별로 재고 일괄 차감 (각 상품당 3개 * 10 = 30)
            productRepository.save(product1.decreaseStock(new Quantity(30L)));
            productRepository.save(product2.decreaseStock(new Quantity(30L)));
            productRepository.save(product3.decreaseStock(new Quantity(30L)));

            // When - 각 상품별로 3개씩 동시 결제 실패 처리
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        Order order = concurrentOrders.get(index);

                        Payment payment = Payment.create(
                                order.getOrderKey(),
                                testUser.getId(),
                                null,
                                null,
                                null
                        );

                        FailedReason failedReason = new FailedReason("결제 실패 #" + index);
                        failedPaymentStrategy.pay(payment, failedReason);
                    }
            );

            // Then - 각 상품의 재고가 독립적으로 정확하게 복구되었는지 검증
            Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            Product finalProduct3 = productRepository.findById(product3.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(finalProduct1.getStock().value())
                        .as("상품1: 3개 결제 실패로 30개 복구 (초기 %d → 현재 %d)",
                                product1InitialStock, finalProduct1.getStock().value())
                        .isEqualTo(product1InitialStock);
                softly.assertThat(finalProduct2.getStock().value())
                        .as("상품2: 3개 결제 실패로 30개 복구 (초기 %d → 현재 %d)",
                                product2InitialStock, finalProduct2.getStock().value())
                        .isEqualTo(product2InitialStock);
                softly.assertThat(finalProduct3.getStock().value())
                        .as("상품3: 3개 결제 실패로 30개 복구 (초기 %d → 현재 %d)",
                                product3InitialStock, finalProduct3.getStock().value())
                        .isEqualTo(product3InitialStock);
            });
        }
    }
}
