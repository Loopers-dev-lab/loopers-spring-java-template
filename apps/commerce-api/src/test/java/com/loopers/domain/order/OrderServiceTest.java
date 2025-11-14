package com.loopers.domain.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockService stockService;

    @Mock
    private PointService pointService;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private Stock stock;
    private Point point;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("testuser1")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();

        Brand brand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .status(BrandStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        stock = Stock.builder()
                .quantity(100L)
                .product(null)
                .build();

        product = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10000))
                .likeCount(0L)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .brand(brand)
                .stock(null)
                .build();

        stock.setProduct(product);
        product.setStock(stock);

        point = Point.builder()
                .amount(BigDecimal.valueOf(50000))
                .user(user)
                .build();

        user.setPoint(point);
        point.setUser(user);
    }

    @DisplayName("정상 주문 흐름")
    @Nested
    class NormalOrderFlow {

        @Test
        @DisplayName("성공: 정상적인 주문 생성")
        void createOrder_success() {
            // given
            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(product.getId(), 2)
            );

            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            doNothing().when(stockService).decreaseQuantity(anyLong(), anyLong());
            when(pointService.findByUserLoginId(any())).thenReturn(Optional.of(point));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                return Optional.of(order);
            });

            // when
            Order result = orderService.createOrder(user, requests);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(result.getOrderItems()).hasSize(1);
            assertThat(result.getFinalAmount()).isEqualByComparingTo(BigDecimal.valueOf(20000));

            verify(stockService, times(1)).decreaseQuantity(anyLong(), eq(2L));
            verify(pointService, times(1)).findByUserLoginId(user.getLoginId());
            verify(orderRepository, times(1)).save(any(Order.class));
        }
    }

    @DisplayName("예외 주문 흐름")
    @Nested
    class ExceptionOrderFlow {

        @Test
        @DisplayName("실패: 상품을 찾을 수 없음")
        void createOrder_productNotFound() {
            // given
            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(999L, 2)
            );

            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(user, requests))
                    .isInstanceOf(CoreException.class)
                    .satisfies(exception -> {
                        CoreException coreException = (CoreException) exception;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
                    });

            verify(stockService, never()).decreaseQuantity(anyLong(), anyLong());
            verify(pointService, never()).findByUserLoginId(any());
        }

        @Test
        @DisplayName("실패: 판매 불가능한 상품")
        void createOrder_productNotSellable() {
            // given
            Product unsellableProduct = Product.builder()
                    .name("Unsellable Product")
                    .description("Test")
                    .price(BigDecimal.valueOf(10000))
                    .likeCount(0L)
                    .status(ProductStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(false) // 판매 불가
                    .build();

            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(unsellableProduct.getId(), 2)
            );

            when(productRepository.findById(anyLong())).thenReturn(Optional.of(unsellableProduct));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(user, requests))
                    .isInstanceOf(CoreException.class)
                    .satisfies(exception -> {
                        CoreException coreException = (CoreException) exception;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    });

            verify(stockService, never()).decreaseQuantity(anyLong(), anyLong());
        }

        @Test
        @DisplayName("실패: 재고 부족")
        void createOrder_insufficientStock() {
            // given
            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(product.getId(), 200) // 재고보다 많은 수량
            );

            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            doThrow(new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다."))
                    .when(stockService).decreaseQuantity(anyLong(), anyLong());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(user, requests))
                    .isInstanceOf(CoreException.class)
                    .satisfies(exception -> {
                        CoreException coreException = (CoreException) exception;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    });

            verify(pointService, never()).findByUserLoginId(any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패: 포인트 부족")
        void createOrder_insufficientPoint() {
            // given
            Point lowPoint = Point.builder()
                    .amount(BigDecimal.valueOf(1000)) // 주문 금액보다 적은 포인트
                    .user(user)
                    .build();

            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(product.getId(), 2) // 20000원 주문
            );

            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            doNothing().when(stockService).decreaseQuantity(anyLong(), anyLong());
            when(pointService.findByUserLoginId(any())).thenReturn(Optional.of(lowPoint));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(user, requests))
                    .isInstanceOf(CoreException.class)
                    .satisfies(exception -> {
                        CoreException coreException = (CoreException) exception;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    });

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("실패: 포인트를 찾을 수 없음")
        void createOrder_pointNotFound() {
            // given
            List<OrderService.OrderItemRequest> requests = List.of(
                    new OrderService.OrderItemRequest(product.getId(), 2)
            );

            when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
            doNothing().when(stockService).decreaseQuantity(anyLong(), anyLong());
            when(pointService.findByUserLoginId(any())).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(user, requests))
                    .isInstanceOf(CoreException.class)
                    .satisfies(exception -> {
                        CoreException coreException = (CoreException) exception;
                        assertThat(coreException.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
                    });

            verify(orderRepository, never()).save(any(Order.class));
        }
    }
}

