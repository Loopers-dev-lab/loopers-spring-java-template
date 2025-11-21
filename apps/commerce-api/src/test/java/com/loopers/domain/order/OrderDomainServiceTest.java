package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDomainServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PointRepository pointRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks OrderDomainService service;

    private OrderItem item(long productId, int quantity, String unitPrice) {
        return OrderItem.builder()
                .order(null)
                .productId(productId)
                .quantity(quantity)
                .price(new BigDecimal(unitPrice))
                .build();
    }

    @Test
    @DisplayName("재고가 존재하지 않으면 주문은 실패한다")
    void fails_when_product_not_found() {
        // given
        long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        var items = List.of(item(productId, 1, "1000"));

        // when
        CoreException ex = assertThrows(CoreException.class, () -> service.createOrder("u1", items));

        // then
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("재고가 부족하면 주문은 실패한다")
    void fails_when_stock_insufficient() {
        // given
        long productId = 1L;
        Product product = mock(Product.class);
        when(product.getStockQuantity()).thenReturn(0); // 부족
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        var items = List.of(item(productId, 2, "1000"));

        // when
        CoreException ex = assertThrows(CoreException.class, () -> service.createOrder("u1", items));

        // then
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트가 부족하면 주문은 실패한다")
    void fails_when_point_insufficient() {
        // given (재고 충분)
        long productId = 1L;
        Product product = mock(Product.class);
        when(product.getStockQuantity()).thenReturn(10);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Point point = mock(Point.class);
        when(point.getPointAmount()).thenReturn(new BigDecimal("500")); // 총액 1000보다 작음
        when(pointRepository.findByUserIdForUpdate("u1")).thenReturn(Optional.of(point));

        var items = List.of(item(productId, 1, "1000"));

        // when
        CoreException ex = assertThrows(CoreException.class, () -> service.createOrder("u1", items));

        // then
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        verify(orderRepository, never()).save(any());
    }
}


