package com.loopers.core.service.payment;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.service.payment.command.PgCallbackCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PgCallbackService {

    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public void callback(PgCallbackCommand command) {
        TransactionKey transactionKey = new TransactionKey(command.transactionKey());
        OrderKey orderKey = new OrderKey(command.orderId());
        PaymentStatus status = PaymentStatus.valueOf(command.status());
        Payment payment = paymentRepository.getByWithLock(transactionKey);

        boolean hasSuccessfulPayment = paymentRepository.findBy(orderKey, PaymentStatus.SUCCESS).isPresent();
        if (hasSuccessfulPayment) {
            paymentRepository.save(payment.fail(new FailedReason("이미 결제에 성공한 이력이 있는 주문입니다.")));
            return;
        }

        if (status != PaymentStatus.SUCCESS) {
            paymentRepository.save(payment.fail(new FailedReason(command.reason())));
            return;
        }

        //재고 차감
        Order order = orderRepository.getBy(orderKey);
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        orderItems.forEach(orderItem -> {
            Product product = productRepository.getByIdWithLock(orderItem.getProductId());
            productRepository.save(product.decreaseStock(orderItem.getQuantity()));
        });

        //결제 성공 업데이트
        paymentRepository.save(payment.success());
    }
}
