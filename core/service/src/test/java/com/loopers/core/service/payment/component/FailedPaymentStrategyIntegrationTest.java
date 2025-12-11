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
}
