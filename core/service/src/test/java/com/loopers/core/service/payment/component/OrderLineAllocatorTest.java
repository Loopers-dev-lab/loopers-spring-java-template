package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.OrderItemFixture;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("OrderLineAllocator 테스트")
@ExtendWith(MockitoExtension.class)
class OrderLineAllocatorTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private EventOutboxRepository eventOutboxRepository;

    @InjectMocks
    private OrderLineAllocator orderLineAllocator;

    @Nested
    @DisplayName("allocate() 메서드")
    class AllocateTest {

        @Nested
        @DisplayName("주문 수량만큼 상품을 차감했을 때 재고가 0이 되면")
        class WhenStockBecomesZero {

            private Product product;
            private OrderItem orderItem;

            @BeforeEach
            void setUp() {
                // Given: 재고가 정확히 10개인 상품
                product = ProductFixture.createWith(
                        new ProductId("1"),
                        new BrandId("1"),
                        new ProductName("테스트 상품"),
                        new ProductPrice(new BigDecimal(10000)),
                        new ProductStock(10L)
                );

                // 주문 수량도 10개로 설정하여 재고가 0이 되도록
                orderItem = OrderItemFixture.createWith(
                        new OrderId("1"),
                        product.getId(),
                        10L
                );

                when(productRepository.getByIdWithLock(product.getId()))
                        .thenReturn(product);
            }

            @Test
            @DisplayName("재고가 0이 되면 OutOfStock 이벤트가 발행되어야 한다")
            void shouldPublishOutOfStockEvent() {
                // JacksonUtil.convertToString() 메서드를 mock으로 처리
                try (MockedStatic<JacksonUtil> mockedStatic = mockStatic(JacksonUtil.class)) {
                    mockedStatic.when(() -> JacksonUtil.convertToString(any()))
                            .thenReturn("{}");

                    // When
                    orderLineAllocator.allocate(orderItem);

                    // Then
                    verify(eventOutboxRepository, times(1)).save(any(EventOutbox.class));
                }
            }
        }

        @Nested
        @DisplayName("주문 수량 이후에도 재고가 남으면")
        class WhenStockRemains {

            private Product product;
            private OrderItem orderItem;

            @BeforeEach
            void setUp() {
                // Given: 재고가 충분한 상품
                product = ProductFixture.createWith(
                        new ProductId("2"),
                        new BrandId("2"),
                        new ProductName("충분한 재고 상품"),
                        new ProductPrice(new BigDecimal(20000)),
                        new ProductStock(100L)
                );

                // 주문 수량은 10개로 설정하여 재고가 남도록
                orderItem = OrderItemFixture.createWith(
                        new OrderId("2"),
                        product.getId(),
                        10L
                );

                when(productRepository.getByIdWithLock(product.getId()))
                        .thenReturn(product);
            }

            @Test
            @DisplayName("재고가 남으면 OutOfStock 이벤트가 발행되지 않아야 한다")
            void shouldNotPublishOutOfStockEvent() {

                // When
                orderLineAllocator.allocate(orderItem);

                // Then
                verify(eventOutboxRepository, never()).save(any(EventOutbox.class));
            }
        }
    }
}
