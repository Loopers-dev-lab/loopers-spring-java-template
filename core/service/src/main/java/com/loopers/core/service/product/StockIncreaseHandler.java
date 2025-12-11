package com.loopers.core.service.product;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentFailedEvent;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.service.product.component.StockIncreaseFailHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockIncreaseHandler {

    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final StockIncreaseFailHandler stockIncreaseFailHandler;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handle(PaymentFailedEvent event) {
        try {
            Payment payment = paymentRepository.getById(event.paymentId());
            Order order = orderRepository.getBy(payment.getOrderKey());

            List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
            List<Product> products = orderItems.stream()
                    .map(orderItem -> {
                        Product product = productRepository.getByIdWithLock(orderItem.getProductId());

                        return product.increaseStock(orderItem.getQuantity());
                    }).toList();

            productRepository.saveAll(products);
        } catch (Exception exception) {
            log.error("재고 원복 처리중 오류가 발생했습니다.", exception);
            stockIncreaseFailHandler.handle(event, exception);
        }
    }
}
