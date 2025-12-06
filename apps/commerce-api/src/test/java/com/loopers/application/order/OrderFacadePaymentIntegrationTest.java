package com.loopers.application.order;

import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentDomainService;
import com.loopers.domain.order.PaymentStatus;
import com.loopers.domain.order.PaymentType;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointAccountDomainService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.order.OrderDto;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderFacadePaymentIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private PaymentDomainService paymentDomainService;

    @Autowired
    private PointAccountDomainService pointAccountDomainService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("포인트만 결제 - 잔액 충분하면 주문 완료")
    void paymentTest1() {
        // arrange
        String userId = "test-user-1";
        pointAccountDomainService.createForUser(userId);
        long pointAmount = 100000L;
        pointAccountDomainService.charge(userId, pointAmount);

        long productPrice = 10000L;
        long quantity = 2L;
        long totalAmount = productPrice * quantity;

        Product product = Product.create("Test Product", "Test Description", productPrice, 100L, null);
        productRepository.save(product);

        List<OrderDto.OrderItemRequest> items = List.of(
            new OrderDto.OrderItemRequest(product.getId(), quantity)
        );

        // act
        OrderInfo result = orderFacade.createOrder(userId, items, null, null);

        // assert
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED.name());
        assertThat(result.totalAmount()).isEqualTo(totalAmount);

        Payment payment = paymentDomainService.getPaymentByOrderId(result.orderId());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT_ONLY);

        Point balance = pointAccountDomainService.getBalance(userId);
        assertThat(balance.amount()).isEqualTo(pointAmount - totalAmount);
    }

    @Test
    @DisplayName("포인트만 결제 - 잔액 부족하면 실패")
    void paymentTest2() {
        // arrange
        String userId = "test-user-2";
        pointAccountDomainService.createForUser(userId);
        long pointAmount = 5000L;
        pointAccountDomainService.charge(userId, pointAmount);

        long productPrice = 10000L;
        long quantity = 2L;
        Product product = Product.create("Test Product 2", "Test Description", productPrice, 100L, null);
        productRepository.save(product);

        List<OrderDto.OrderItemRequest> items = List.of(
            new OrderDto.OrderItemRequest(product.getId(), quantity)
        );

        // act & assert
        assertThatThrownBy(() ->
            orderFacade.createOrder(userId, items, null, null)
        ).isInstanceOf(CoreException.class)
         .hasMessageContaining("포인트가 부족합니다");
    }

    @Test
    @DisplayName("카드 정보 없으면 포인트 결제 (기본값)")
    void paymentTest3() {
        // arrange
        String userId = "test-user-3";
        pointAccountDomainService.createForUser(userId);
        pointAccountDomainService.charge(userId, 100000L);

        long productPrice = 10000L;
        long quantity = 2L;
        Product product = Product.create("Test Product 3", "Test Description", productPrice, 100L, null);
        productRepository.save(product);

        List<OrderDto.OrderItemRequest> items = List.of(
            new OrderDto.OrderItemRequest(product.getId(), quantity)
        );

        // act
        OrderInfo result = orderFacade.createOrder(userId, items, null, null);

        // assert
        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED.name());

        Payment payment = paymentDomainService.getPaymentByOrderId(result.orderId());
        assertThat(payment.getPaymentType()).isEqualTo(PaymentType.POINT_ONLY);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

}
