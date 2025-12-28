package com.loopers.core.service.product;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.product.DailyProductMetric;
import com.loopers.core.domain.product.repository.ProductMetricRepository;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductTotalSalesCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IncreaseProductTotalSalesService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductMetricRepository productMetricRepository;

    @InboxEvent(
            aggregateType = "PAYMENT",
            eventType = "INCREASE_TOTAL_SALES_COUNT",
            eventIdField = "eventId",
            aggregateIdField = "paymentId"
    )
    @Transactional
    public void increase(IncreaseProductTotalSalesCommand command) {
        Payment payment = paymentRepository.getById(new PaymentId(command.paymentId()));
        Order order = orderRepository.getBy(payment.getOrderKey());
        orderItemRepository.findAllByOrderId(order.getId()).forEach(orderItem -> {
            DailyProductMetric metric = productMetricRepository.findByWithLock(orderItem.getProductId(), new CreatedAt(LocalDateTime.now()))
                    .orElse(DailyProductMetric.init(orderItem.getProductId()));

            productMetricRepository.save(metric.increaseSalesCount(orderItem.getQuantity()));
        });
    }
}
