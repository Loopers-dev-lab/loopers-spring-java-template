package com.loopers.core.service.order;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.component.OrderLineAllocator;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@DisplayName("OrderLineAllocator 통합 테스트")
class OrderLineAllocatorIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderLineAllocator orderLineAllocator;

    @Autowired
    private ProductRepository productRepository;

    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        // 상품 생성 및 저장 (가격: 10,000, 재고: 100개)
        product = Product.mappedBy(
                ProductId.empty(),
                new BrandId("1"),
                new ProductName("테스트상품"),
                new ProductPrice(new BigDecimal("10000")),
                new ProductStock(100L),
                ProductLikeCount.init(),
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
        product = productRepository.save(product);

        // 주문 상품 생성 (수량: 5개)
        orderItem = OrderItem.create(
            new OrderId("1"),
            product.getProductId(),
            new Quantity(5L)
        );
    }

    @Nested
    @DisplayName("allocate 메서드")
    class AllocateMethod {

        @Test
        @DisplayName("상품의 재고를 차감하고 총 가격을 반환한다")
        void allocateSuccess() {
            // when
            BigDecimal totalPrice = orderLineAllocator.allocate(orderItem);

            // then - 반환된 가격 검증 (10,000 * 5 = 50,000)
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("50000"));
            });

            // 상품 재고 차감 확인
            Product allocatedProduct = productRepository.getById(product.getProductId());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(allocatedProduct.getStock().value()).isEqualTo(95L); // 100 - 5
            });
        }

        @Test
        @DisplayName("여러 번 할당하면 재고가 누적으로 차감된다")
        void allocateMultipleTimes() {
            // when - 첫 번째 할당 (5개)
            orderLineAllocator.allocate(orderItem);

            // 두 번째 할당 (5개)
            orderLineAllocator.allocate(orderItem);

            // then - 재고 누적 차감 확인
            Product allocatedProduct = productRepository.getById(product.getProductId());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(allocatedProduct.getStock().value()).isEqualTo(90L); // 100 - 5 - 5
            });
        }

        @Test
        @DisplayName("재고 부족 시 예외를 발생시킨다")
        void throwExceptionWhenInsufficientStock() {
            // given - 재고보다 많은 수량 주문
            OrderItem largeOrderItem = OrderItem.create(
                    new OrderId("1"),
                    product.getProductId(),
                    new Quantity(150L)
            );

            // when & then
            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () -> orderLineAllocator.allocate(largeOrderItem)
                    ).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품의 재고가 부족합니다.");
        }

        @Test
        @DisplayName("정확히 남은 재고만큼 할당하면 재고가 0이 된다")
        void allocateWithExactStock() {
            // given - 정확히 현재 재고만큼 주문
            OrderItem exactOrderItem = OrderItem.create(
                    new OrderId("1"),
                    product.getProductId(),
                    new Quantity(100L)
            );

            // when
            BigDecimal totalPrice = orderLineAllocator.allocate(exactOrderItem);

            // then - 총 가격 검증 (10,000 * 100 = 1,000,000)
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(totalPrice).isEqualByComparingTo(new BigDecimal("1000000"));
            });

            // 재고가 0이 되었는지 확인
            Product allocatedProduct = productRepository.getById(product.getProductId());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(allocatedProduct.getStock().value()).isZero();
            });
        }
    }
}
