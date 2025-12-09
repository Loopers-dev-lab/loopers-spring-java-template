package com.loopers.core.service.payment.component;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderFixture;
import com.loopers.core.domain.order.OrderItemFixture;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PaymentFixture;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.PgPaymentFixture;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserFixture;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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

    @Autowired
    private PaymentRepository paymentRepository;

    private User testUser;
    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        testUser = userRepository.save(UserFixture.create());

        // 상품 생성 (재고: 1,000개)
        testProduct = productRepository.save(
                ProductFixture.createWith(
                        new BrandId("1"),
                        new ProductName("테스트 상품"),
                        new ProductPrice(new BigDecimal("10000")),
                        new ProductStock(1000L)
                )
        );

        // 주문 생성
        testOrder = orderRepository.save(OrderFixture.createWith(testUser.getId()));
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

            // 주문 상품 생성
            orderItemRepository.save(
                    OrderItemFixture.createWith(testOrder.getId(), testProduct.getId(), 10L)
            );

            Product decreasedProduct = testProduct.decreaseStock(new Quantity(10L));
            productRepository.save(decreasedProduct);

            Payment savedPayment = paymentRepository.save(
                    PaymentFixture.createWith(testOrder.getOrderKey(), testUser.getId())
            );

            PgPayment pgPayment = PgPaymentFixture.createWith(savedPayment.getId());

            // When
            failedPaymentStrategy.pay(pgPayment, failedReason);

            // Then
            Product restoredProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            Payment updatedPayment = paymentRepository.getById(savedPayment.getId());

            assertSoftly(softly -> {
                softly.assertThat(restoredProduct.getStock().value())
                        .as("재고가 원래 상태로 복구되어야 함")
                        .isEqualTo(initialStock);
                softly.assertThat(updatedPayment.getFailedReason().value())
                        .as("결제의 실패 이유가 저장되어야 함")
                        .isEqualTo("결제 게이트웨이 오류");
            });
        }

        @Test
        @DisplayName("여러 상품의 재고를 모두 정확하게 복구한다")
        void shouldRestoreStockForMultipleItems() {
            // Given
            Product product2 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("2"),
                            new ProductName("테스트 상품 2"),
                            new ProductPrice(new BigDecimal("15000")),
                            new ProductStock(1000L)
                    )
            );

            // 주문 상품 생성
            orderItemRepository.save(OrderItemFixture.createWith(testOrder.getId(), testProduct.getId(), 10L));
            orderItemRepository.save(OrderItemFixture.createWith(testOrder.getId(), product2.getId(), 20L));

            long product1InitialStock = testProduct.getStock().value();
            long product2InitialStock = product2.getStock().value();

            Product decreasedProduct1 = testProduct.decreaseStock(new Quantity(10L));
            Product decreasedProduct2 = product2.decreaseStock(new Quantity(20L));
            productRepository.saveAll(List.of(decreasedProduct1, decreasedProduct2));

            Payment savedPayment = paymentRepository.save(PaymentFixture.createWith(testOrder.getOrderKey(), testUser.getId()));

            PgPayment pgPayment = PgPaymentFixture.createWith(savedPayment.getId());

            FailedReason failedReason = new FailedReason("결제 처리 중 오류 발생");

            // When
            failedPaymentStrategy.pay(pgPayment, failedReason);

            // Then
            Product restoredProduct1 = productRepository.findById(testProduct.getId()).orElseThrow();
            Product restoredProduct2 = productRepository.findById(product2.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(restoredProduct1.getStock().value())
                        .as("첫 번째 상품 재고가 원래대로 복구되어야 함")
                        .isEqualTo(product1InitialStock);
                softly.assertThat(restoredProduct2.getStock().value())
                        .as("두 번째 상품 재고가 원래대로 복구되어야 함")
                        .isEqualTo(product2InitialStock);
            });
        }

        @Test
        @DisplayName("결제 실패 시 올바른 실패 이유를 저장한다")
        void shouldSaveCorrectFailedReasonInPayment() {
            // Given
            String expectedReason = "신용카드 한도 초과";
            FailedReason failedReason = new FailedReason(expectedReason);

            // 주문 상품 생성
            orderItemRepository.save(
                    OrderItemFixture.createWith(testOrder.getId(), testProduct.getId(), 10L)
            );

            Product decreasedProduct = testProduct.decreaseStock(new Quantity(10L));
            productRepository.save(decreasedProduct);

            Payment savedPayment = paymentRepository.save(PaymentFixture.createWith(testOrder.getOrderKey(), testUser.getId()));

            PgPayment pgPayment = PgPaymentFixture.createWith(savedPayment.getId());


            // When
            failedPaymentStrategy.pay(pgPayment, failedReason);

            // Then
            Payment updatedPayment = paymentRepository.getById(savedPayment.getId());
            assertThat(updatedPayment.getFailedReason().value())
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

            List<Payment> concurrentPayments = new ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                Order order = orderRepository.save(
                        OrderFixture.createWith(testUser.getId())
                );
                orderItemRepository.save(
                        OrderItemFixture.createWith(order.getId(), testProduct.getId(), stockPerPayment)
                );

                concurrentPayments.add(
                        paymentRepository.save(PaymentFixture.createWith(order.getOrderKey(), testUser.getId()))
                );
            }

            Product decreasedProduct = testProduct.decreaseStock(new Quantity((long) (concurrentFailedPaymentCount * stockPerPayment)));
            productRepository.save(decreasedProduct);

            List<PgPayment> pgPayments = new ArrayList<>();
            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                pgPayments.add(
                        PgPaymentFixture.createWith(concurrentPayments.get(i).getId())
                );
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PgPayment pgPayment = pgPayments.get(index);
                        FailedReason failedReason = new FailedReason("결제 게이트웨이 오류 #" + index);
                        failedPaymentStrategy.pay(pgPayment, failedReason);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().value())
                    .as("동시 결제 실패로 인한 재고 복구가 정확하게 이루어져야 함")
                    .isEqualTo(initialStock);
        }

        @Test
        @DisplayName("10개의 동시 결제 실패에서 재고 복구 일관성을 보장한다")
        void shouldMaintainStockConsistencyWith10ConcurrentFailedPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentFailedPaymentCount = 10;
            int stockPerPayment = 5;

            ArrayList<Payment> concurrentPayments = new ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                Order order = orderRepository.save(
                        OrderFixture.createWith(testUser.getId())
                );
                orderItemRepository.save(
                        OrderItemFixture.createWith(order.getId(), testProduct.getId(), stockPerPayment)
                );

                concurrentPayments.add(
                        paymentRepository.save(PaymentFixture.createWith(order.getOrderKey(), testUser.getId()))
                );
            }

            Product decreasedProduct = testProduct.decreaseStock(new Quantity((long) (concurrentFailedPaymentCount * stockPerPayment)));
            productRepository.save(decreasedProduct);

            List<PgPayment> pgPayments = new ArrayList<>();
            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                pgPayments.add(
                        PgPaymentFixture.createWith(concurrentPayments.get(i).getId())
                );
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PgPayment pgPayment = pgPayments.get(index);
                        FailedReason failedReason = new FailedReason("결제 실패: " + index);
                        failedPaymentStrategy.pay(pgPayment, failedReason);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().value())
                    .as("10개의 동시 결제 실패로 인한 재고 복구 일관성을 보장")
                    .isEqualTo(initialStock);
        }

        @Test
        @DisplayName("서로 다른 상품에 대한 동시 결제 실패에서 각 상품의 재고가 독립적으로 복구된다")
        void shouldRestoreStockIndependentlyForDifferentProducts() throws InterruptedException {
            // Given
            Product product1 = testProduct;
            Product product2 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("2"),
                            new ProductName("테스트 상품 2"),
                            new ProductPrice(new BigDecimal("15000")),
                            new ProductStock(1000L)
                    )
            );
            Product product3 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("3"),
                            new ProductName("테스트 상품 3"),
                            new ProductPrice(new BigDecimal("20000")),
                            new ProductStock(1000L)
                    )
            );

            long product1InitialStock = product1.getStock().value();
            long product2InitialStock = product2.getStock().value();
            long product3InitialStock = product3.getStock().value();

            int concurrentFailedPaymentCount = 9; // 3개 상품 × 3개 결제씩
            List<Payment> concurrentPayments = new ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                Product targetProduct = switch (i / 3) {
                    case 0 -> product1;
                    case 1 -> product2;
                    default -> product3;
                };

                Order order = orderRepository.save(
                        OrderFixture.createWith(testUser.getId())
                );
                orderItemRepository.save(
                        OrderItemFixture.createWith(order.getId(), targetProduct.getId(), 10L)
                );

                concurrentPayments.add(
                        paymentRepository.save(PaymentFixture.createWith(order.getOrderKey(), testUser.getId()))
                );
            }

            productRepository.save(product1.decreaseStock(new Quantity(30L)));
            productRepository.save(product2.decreaseStock(new Quantity(30L)));
            productRepository.save(product3.decreaseStock(new Quantity(30L)));

            List<PgPayment> pgPayments = new ArrayList<>();
            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                pgPayments.add(
                        PgPaymentFixture.createWith(concurrentPayments.get(i).getId())
                );
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PgPayment pgPayment = pgPayments.get(index);
                        FailedReason failedReason = new FailedReason("결제 실패 #" + index);
                        failedPaymentStrategy.pay(pgPayment, failedReason);
                    }
            );

            // Then
            Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            Product finalProduct3 = productRepository.findById(product3.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(finalProduct1.getStock().value())
                        .as("상품1 재고 복구")
                        .isEqualTo(product1InitialStock);
                softly.assertThat(finalProduct2.getStock().value())
                        .as("상품2 재고 복구")
                        .isEqualTo(product2InitialStock);
                softly.assertThat(finalProduct3.getStock().value())
                        .as("상품3 재고 복구")
                        .isEqualTo(product3InitialStock);
            });
        }
    }
}
