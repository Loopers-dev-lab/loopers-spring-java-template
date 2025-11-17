package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
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
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrderLineAllocator 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderLineAllocatorTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderLineAllocator orderLineAllocator;

    @Nested
    @DisplayName("allocate 메서드")
    class AllocateMethod {

        @Nested
        @DisplayName("충분한 재고가 있을 때")
        class WhenSufficientStock {

            private ProductId productId;
            private Quantity quantity;
            private OrderItem orderItem;
            private Product product;

            @BeforeEach
            void setUp() {
                productId = new ProductId("1");
                quantity = new Quantity(5L);
                orderItem = Instancio.of(OrderItem.class)
                        .set(field(OrderItem::getId), OrderItemId.empty())
                        .set(field(OrderItem::getProductId), productId)
                        .set(field(OrderItem::getQuantity), quantity)
                        .set(field(OrderItem::getOrderId), OrderId.empty())
                        .create();

                product = Instancio.of(Product.class)
                        .set(field(Product::getId), productId)
                        .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                        .set(field(Product::getStock), new ProductStock(100L))
                        .create();

                when(productRepository.getByIdWithLock(productId)).thenReturn(product);
                when(productRepository.save(any(Product.class))).thenReturn(product.decreaseStock(quantity));
                when(orderItemRepository.save(any(OrderItem.class))).thenReturn(orderItem);
            }

            @Test
            @DisplayName("재고를 차감하고 총 가격을 반환한다")
            void allocateAndReturnTotalPrice() {
                // when
                BigDecimal result = orderLineAllocator.allocate(orderItem);

                // then
                assertThat(result).isEqualByComparingTo(new BigDecimal("50000")); // 10000 * 5
                verify(productRepository).save(any(Product.class));
                verify(orderItemRepository).save(any(OrderItem.class));
            }
        }

        @Nested
        @DisplayName("재고가 부족할 때")
        class WhenInsufficientStock {

            private ProductId productId;
            private Quantity quantity;
            private OrderItem orderItem;
            private Product product;

            @BeforeEach
            void setUp() {
                productId = new ProductId("1");
                quantity = new Quantity(101L); // 100개보다 많음
                orderItem = Instancio.of(OrderItem.class)
                        .set(field(OrderItem::getId), OrderItemId.empty())
                        .set(field(OrderItem::getProductId), productId)
                        .set(field(OrderItem::getQuantity), quantity)
                        .set(field(OrderItem::getOrderId), OrderId.empty())
                        .create();

                product = Instancio.of(Product.class)
                        .set(field(Product::getId), productId)
                        .set(field(Product::getPrice), new ProductPrice(new BigDecimal("10000")))
                        .set(field(Product::getStock), new ProductStock(100L))
                        .create();

                when(productRepository.getByIdWithLock(productId)).thenReturn(product);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void throwException() {
                // when & then
                assertThatThrownBy(() -> orderLineAllocator.allocate(orderItem))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("상품의 재고가 부족합니다.");
            }
        }
    }
}
