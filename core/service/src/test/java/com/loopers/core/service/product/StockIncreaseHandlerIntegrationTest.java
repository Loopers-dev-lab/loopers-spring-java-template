package com.loopers.core.service.product;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderFixture;
import com.loopers.core.domain.order.OrderItemFixture;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PaymentFixture;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.PaymentFailedEvent;
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

@DisplayName("재고 증가 핸들러 동시성 테스트")
class StockIncreaseHandlerIntegrationTest extends IntegrationTest {

    @Autowired
    private StockIncreaseHandler stockIncreaseHandler;

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
    }

    @Nested
    @DisplayName("동시성 제어 - 동시 결제 실패 시 재고 증가 검증")
    class ConcurrencyControl {

        @Test
        @DisplayName("같은 상품에 대해 동시에 여러 결제가 실패할 때 재고 증가 일관성을 보장한다")
        void shouldMaintainStockConsistencyUnderConcurrentFailedPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentFailedPaymentCount = 5;
            int stockPerPayment = 10;

            List<Payment> concurrentPayments = new ArrayList<>();
            List<Order> concurrentOrders = new ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                Order order = orderRepository.save(
                        OrderFixture.createWith(testUser.getId())
                );
                concurrentOrders.add(order);

                orderItemRepository.save(
                        OrderItemFixture.createWith(order.getId(), testProduct.getId(), stockPerPayment)
                );

                concurrentPayments.add(
                        paymentRepository.save(PaymentFixture.createWith(order.getOrderKey(), testUser.getId()))
                );
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PaymentFailedEvent event = new PaymentFailedEvent(
                                concurrentPayments.get(index).getId(),
                                new FailedReason("결제 게이트웨이 오류 #" + index)
                        );
                        stockIncreaseHandler.handle(event);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock + (long) concurrentFailedPaymentCount * stockPerPayment;
            assertThat(finalProduct.getStock().value())
                    .as("동시 결제 실패로 인한 재고 증가가 정확하게 이루어져야 함")
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("10개의 동시 결제 실패에서 재고 증가 일관성을 보장한다")
        void shouldMaintainStockConsistencyWith10ConcurrentFailedPayments() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentFailedPaymentCount = 10;
            int stockPerPayment = 5;

            List<Payment> concurrentPayments = new ArrayList<>();
            List<Order> concurrentOrders = new ArrayList<>();

            for (int i = 0; i < concurrentFailedPaymentCount; i++) {
                Order order = orderRepository.save(
                        OrderFixture.createWith(testUser.getId())
                );
                concurrentOrders.add(order);

                orderItemRepository.save(
                        OrderItemFixture.createWith(order.getId(), testProduct.getId(), stockPerPayment)
                );

                concurrentPayments.add(
                        paymentRepository.save(PaymentFixture.createWith(order.getOrderKey(), testUser.getId()))
                );
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PaymentFailedEvent event = new PaymentFailedEvent(
                                concurrentPayments.get(index).getId(),
                                new FailedReason("결제 실패: " + index)
                        );
                        stockIncreaseHandler.handle(event);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock + (long) concurrentFailedPaymentCount * stockPerPayment;
            assertThat(finalProduct.getStock().value())
                    .as("10개의 동시 결제 실패로 인한 재고 증가 일관성을 보장")
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("서로 다른 상품에 대한 동시 결제 실패에서 각 상품의 재고가 독립적으로 증가된다")
        void shouldIncreaseStockIndependentlyForDifferentProducts() throws InterruptedException {
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

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentFailedPaymentCount,
                    index -> {
                        PaymentFailedEvent event = new PaymentFailedEvent(
                                concurrentPayments.get(index).getId(),
                                new FailedReason("결제 실패 #" + index)
                        );
                        stockIncreaseHandler.handle(event);
                    }
            );

            // Then
            Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            Product finalProduct3 = productRepository.findById(product3.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(finalProduct1.getStock().value())
                        .as("상품1 재고 증가")
                        .isEqualTo(product1InitialStock + 30);
                softly.assertThat(finalProduct2.getStock().value())
                        .as("상품2 재고 증가")
                        .isEqualTo(product2InitialStock + 30);
                softly.assertThat(finalProduct3.getStock().value())
                        .as("상품3 재고 증가")
                        .isEqualTo(product3InitialStock + 30);
            });
        }
    }
}
