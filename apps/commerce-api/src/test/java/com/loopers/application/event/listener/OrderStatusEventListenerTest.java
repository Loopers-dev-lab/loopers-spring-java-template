package com.loopers.application.event.listener;

import com.loopers.application.event.payment.PaymentCompletedEvent;
import com.loopers.domain.order.InMemoryOrderRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.InMemoryProductRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("주문 상태 이벤트 리스너 테스트")
class OrderStatusEventListenerTest {

    private OrderStatusEventListener orderStatusEventListener;
    private InMemoryOrderRepository orderRepository;
    private InMemoryProductRepository productRepository;
    private ApplicationEventPublisher eventPublisher;

    private Order testOrder;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        productRepository = new InMemoryProductRepository();
        eventPublisher = mock(ApplicationEventPublisher.class);

        orderStatusEventListener = new OrderStatusEventListener(
            orderRepository,
            productRepository,
            eventPublisher
        );

        // 테스트 상품 생성
        testProduct = new Product(1L, "테스트상품", null, Money.of(10000), Stock.of(100));
        productRepository.save(testProduct);

        // 테스트 주문 생성 (결제 대기 상태)
        OrderItem orderItem = new OrderItem(testProduct.getId(), 1, testProduct.getPrice());
        testOrder = Order.create(1L, List.of(orderItem), Money.of(10000));
        orderRepository.save(testOrder);
    }

    @Test
    @DisplayName("결제 성공 시 주문 상태가 PAID로 변경된다")
    void 결제_성공_시_주문_완료() {
        // given
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            testOrder.getOrderNo(),
            1L,
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now()
        );

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        // when
        orderStatusEventListener.handlePaymentCompleted(event);

        // then
        Order updatedOrder = orderRepository.findByOrderNo(testOrder.getOrderNo()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(updatedOrder.isPaid()).isTrue();
    }

    @Test
    @DisplayName("결제 실패 시 주문이 취소되고 재고가 복구된다")
    void 결제_실패_시_주문_취소_및_재고_복구() {
        // given
        int originalStock = testProduct.getStock().getQuantity();

        PaymentCompletedEvent event = new PaymentCompletedEvent(
            testOrder.getOrderNo(),
            1L,
            PaymentStatus.FAILED,
            "카드 한도 초과",
            LocalDateTime.now()
        );

        // when
        orderStatusEventListener.handlePaymentCompleted(event);

        // then
        Order order = orderRepository.findByOrderNo(testOrder.getOrderNo()).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.isCancelled()).isTrue();

        // 재고가 복구되어야 함
        Product product = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(product.getStock().getQuantity()).isEqualTo(originalStock + 1);
    }

    @Test
    @DisplayName("이미 결제 완료된 주문은 중복 처리되지 않는다")
    void 이미_결제완료된_주문_중복_처리_방지() {
        // given
        testOrder.markAsPaid();
        orderRepository.save(testOrder);

        PaymentCompletedEvent event = new PaymentCompletedEvent(
            testOrder.getOrderNo(),
            1L,
            PaymentStatus.SUCCESS,
            null,
            LocalDateTime.now()
        );

        // when
        orderStatusEventListener.handlePaymentCompleted(event);

        // then
        Order order = orderRepository.findByOrderNo(testOrder.getOrderNo()).orElseThrow();
        assertThat(order.isPaid()).isTrue();
    }
}
