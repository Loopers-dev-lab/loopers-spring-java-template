package com.loopers.core.service.payment.component;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FailedPaymentStrategy implements PaymentCallbackStrategy {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void pay(PgPayment pgPayment, FailedReason failedReason) {
        Payment payment = paymentRepository.getById(pgPayment.getPaymentId());
        Order order = orderRepository.getBy(payment.getOrderKey());

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        List<Product> products = orderItems.stream()
                .map(orderItem -> {
                    Product product = productRepository.getByIdWithLock(orderItem.getProductId());

                    return product.increaseStock(orderItem.getQuantity());
                }).toList();

        productRepository.saveAll(products);
        paymentRepository.save(payment.failed(failedReason));
    }
}
