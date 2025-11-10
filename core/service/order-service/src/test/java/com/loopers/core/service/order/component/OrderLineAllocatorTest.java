package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("주문 라인 할당기")
@ExtendWith(MockitoExtension.class)
class OrderLineAllocatorTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderLineAllocator orderLineAllocator;

    @Nested
    @DisplayName("allocate 메서드")
    class AllocateMethod {

        @Test
        @DisplayName("충분한 재고가 있으면 재고를 차감하고 총 가격을 반환한다")
        void allocateWithSufficientStock() {
            // given
            ProductId productId = new ProductId("1");
            Quantity quantity = new Quantity(5L);
            OrderedProduct orderedProduct = new OrderedProduct(productId, quantity);

            Product product = Product.mappedBy(
                productId,
                null,
                null,
                new ProductPrice(new BigDecimal("10000")),
                new ProductStock(100L),
                null,
                null,
                null,
                null
            );

            when(productRepository.getById(productId)).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product.decreaseStock(quantity));

            // when
            BigDecimal result = orderLineAllocator.allocate(orderedProduct);

            // then
            assertThat(result).isEqualByComparingTo(new BigDecimal("50000")); // 10000 * 5
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("재고가 부족하면 예외를 발생시킨다")
        void throwExceptionWhenInsufficientStock() {
            // given
            ProductId productId = new ProductId("1");
            Quantity quantity = new Quantity(101L); // 100개보다 많음
            OrderedProduct orderedProduct = new OrderedProduct(productId, quantity);

            Product product = Product.mappedBy(
                productId,
                null,
                null,
                new ProductPrice(new BigDecimal("10000")),
                new ProductStock(100L),
                null,
                null,
                null,
                null
            );

            when(productRepository.getById(productId)).thenReturn(product);

            // when & then
            assertThatThrownBy(() -> orderLineAllocator.allocate(orderedProduct))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상품의 재고가 부족합니다.");
        }
    }
}
