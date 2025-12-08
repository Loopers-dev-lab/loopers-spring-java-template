package com.loopers.core.service.payment.component;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderFixture;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.OrderItemFixture;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("주문 라인 할당자 통합 테스트")
class OrderLineAllocatorIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderLineAllocator orderLineAllocator;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("allocate 메서드")
    class AllocateMethod {

        private Product testProduct;
        private Order testOrder;

        @BeforeEach
        void setUp() {
            // 사용자 생성
            User testUser = userRepository.save(UserFixture.create());

            // 상품 생성 (가격: 10,000, 재고: 1,000개)
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

        @Test
        @DisplayName("단일 주문 상품의 재고를 차감하면 정확하게 차감된다")
        void shouldDecreaseStockCorrectlyForSingleOrderItem() {
            // Given
            long initialStock = testProduct.getStock().value();
            long quantityToAllocate = 10L;

            OrderItem orderItem = OrderItemFixture.createWith(
                    testOrder.getId(),
                    testProduct.getId(),
                    quantityToAllocate
            );

            // When
            orderLineAllocator.allocate(orderItem);

            // Then
            Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(updatedProduct.getStock().value())
                    .as("재고가 정확하게 차감되어야 함")
                    .isEqualTo(initialStock - quantityToAllocate);
        }

        @Test
        @DisplayName("여러 주문 상품의 재고를 차감하면 모두 정확하게 차감된다")
        void shouldDecreaseStockCorrectlyForMultipleOrderItems() {
            // Given
            long initialStock = testProduct.getStock().value();

            OrderItem orderItem1 = OrderItemFixture.createWith(
                    testOrder.getId(),
                    testProduct.getId(),
                    10L
            );

            OrderItem orderItem2 = OrderItemFixture.createWith(
                    testOrder.getId(),
                    testProduct.getId(),
                    20L
            );

            // When
            orderLineAllocator.allocate(orderItem1);
            orderLineAllocator.allocate(orderItem2);

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            assertThat(finalProduct.getStock().value())
                    .as("모든 주문 상품의 재고가 차감되어야 함 (10 + 20 = 30)")
                    .isEqualTo(initialStock - 30);
        }

        @Test
        @DisplayName("주문 상품을 할당하면 데이터베이스에 저장된다")
        void shouldSaveOrderItemToDatabase() {
            // Given
            OrderItem orderItem = OrderItemFixture.createWith(
                    testOrder.getId(),
                    testProduct.getId(),
                    5L
            );

            // When
            orderLineAllocator.allocate(orderItem);

            // Then
            java.util.List<OrderItem> savedItems = orderItemRepository.findAllByOrderId(testOrder.getId());
            assertThat(savedItems)
                    .as("주문 상품이 데이터베이스에 저장되어야 함")
                    .hasSize(1)
                    .anySatisfy(item -> {
                        assertThat(item.getProductId()).isEqualTo(testProduct.getId());
                        assertThat(item.getQuantity().value()).isEqualTo(5L);
                    });
        }
    }

    @Nested
    @DisplayName("동시성 제어")
    class ConcurrencyControl {

        private Product testProduct;
        private Order testOrder;

        @BeforeEach
        void setUp() {
            // 사용자 생성
            User testUser = userRepository.save(UserFixture.create());

            // 상품 생성 (가격: 10,000, 재고: 1,000개)
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

        @Test
        @DisplayName("같은 상품에 대해 동시에 여러 할당이 발생하면 재고 일관성을 보장한다")
        void shouldMaintainStockConsistencyUnderConcurrentAllocations() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentAllocationCount = 5;
            long quantityPerAllocation = 10L;

            java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();
            for (int i = 0; i < concurrentAllocationCount; i++) {
                OrderItem orderItem = OrderItemFixture.createWith(
                        testOrder.getId(),
                        testProduct.getId(),
                        quantityPerAllocation
                );
                orderItems.add(orderItem);
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentAllocationCount,
                    index -> {
                        OrderItem orderItem = orderItems.get(index);
                        orderLineAllocator.allocate(orderItem);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock - (concurrentAllocationCount * quantityPerAllocation);

            assertThat(finalProduct.getStock().value())
                    .as("동시 할당으로 인한 재고 차감이 정확하게 이루어져야 함 (초기: %d, 현재: %d, 예상: %d)",
                            initialStock, finalProduct.getStock().value(), expectedStock)
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("10개의 동시 할당이 발생하면 재고 일관성을 보장한다")
        void shouldMaintainStockConsistencyWith10ConcurrentAllocations() throws InterruptedException {
            // Given
            long initialStock = testProduct.getStock().value();
            int concurrentAllocationCount = 10;
            long quantityPerAllocation = 5L;

            java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();
            for (int i = 0; i < concurrentAllocationCount; i++) {
                OrderItem orderItem = OrderItemFixture.createWith(
                        testOrder.getId(),
                        testProduct.getId(),
                        quantityPerAllocation
                );
                orderItems.add(orderItem);
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentAllocationCount,
                    index -> {
                        OrderItem orderItem = orderItems.get(index);
                        orderLineAllocator.allocate(orderItem);
                    }
            );

            // Then
            Product finalProduct = productRepository.findById(testProduct.getId()).orElseThrow();
            long expectedStock = initialStock - (concurrentAllocationCount * quantityPerAllocation);

            assertThat(finalProduct.getStock().value())
                    .as("10개의 동시 할당으로 인한 재고 일관성 (초기: %d, 현재: %d, 예상: %d)",
                            initialStock, finalProduct.getStock().value(), expectedStock)
                    .isEqualTo(expectedStock);
        }

        @Test
        @DisplayName("서로 다른 상품에 대한 동시 할당이 발생하면 각 상품의 재고가 독립적으로 관리된다")
        void shouldManageStockIndependentlyForDifferentProducts() throws InterruptedException {
            // Given
            Product product1 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("1"),
                            new ProductName("테스트 상품 1"),
                            new ProductPrice(new BigDecimal("10000")),
                            new ProductStock(1000L)
                    )
            );

            Product product2 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("2"),
                            new ProductName("테스트 상품 2"),
                            new ProductPrice(new BigDecimal("15000")),
                            new ProductStock(800L)
                    )
            );

            Product product3 = productRepository.save(
                    ProductFixture.createWith(
                            new BrandId("3"),
                            new ProductName("테스트 상품 3"),
                            new ProductPrice(new BigDecimal("20000")),
                            new ProductStock(600L)
                    )
            );

            long product1InitialStock = product1.getStock().value();
            long product2InitialStock = product2.getStock().value();
            long product3InitialStock = product3.getStock().value();

            int concurrentAllocationCount = 9;
            java.util.List<OrderItem> orderItems = new java.util.ArrayList<>();

            for (int i = 0; i < concurrentAllocationCount; i++) {
                Product targetProduct = switch (i / 3) {
                    case 0 -> product1;
                    case 1 -> product2;
                    default -> product3;
                };

                OrderItem orderItem = OrderItemFixture.createWith(
                        testOrder.getId(),
                        targetProduct.getId(),
                        10L
                );
                orderItems.add(orderItem);
            }

            // When
            ConcurrencyTestUtil.executeInParallelWithoutResult(
                    concurrentAllocationCount,
                    index -> {
                        OrderItem orderItem = orderItems.get(index);
                        orderLineAllocator.allocate(orderItem);
                    }
            );

            // Then
            Product finalProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product finalProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            Product finalProduct3 = productRepository.findById(product3.getId()).orElseThrow();

            assertSoftly(softly -> {
                softly.assertThat(finalProduct1.getStock().value())
                        .as("상품1: 3개 할당으로 30개 차감 (초기 %d → 현재 %d)",
                                product1InitialStock, finalProduct1.getStock().value())
                        .isEqualTo(product1InitialStock - 30);
                softly.assertThat(finalProduct2.getStock().value())
                        .as("상품2: 3개 할당으로 30개 차감 (초기 %d → 현재 %d)",
                                product2InitialStock, finalProduct2.getStock().value())
                        .isEqualTo(product2InitialStock - 30);
                softly.assertThat(finalProduct3.getStock().value())
                        .as("상품3: 3개 할당으로 30개 차감 (초기 %d → 현재 %d)",
                                product3InitialStock, finalProduct3.getStock().value())
                        .isEqualTo(product3InitialStock - 30);
            });
        }
    }
}
