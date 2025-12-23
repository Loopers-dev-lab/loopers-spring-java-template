package com.loopers.core.service.product;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.service.config.InboxEvent;
import com.loopers.core.service.product.command.IncreaseProductSalesRankingScoreCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncreaseProductSalesRankingScoreService {

    private final ProductRankingCacheRepository productRankingCacheRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${product.ranking.score.weight.pay}")
    private double weight;

    @InboxEvent(
            aggregateType = "PRODUCT",
            eventType = "INCREASE_SALES_RANKING_COUNT",
            eventIdField = "eventId",
            aggregateIdField = "paymentId"
    )
    public void increase(IncreaseProductSalesRankingScoreCommand command) {
        Payment payment = paymentRepository.getById(new PaymentId(command.paymentId()));
        Order order = orderRepository.getBy(payment.getOrderKey());
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        orderItems.forEach(
                item -> productRankingCacheRepository.increaseDaily(item.getProductId(), LocalDateTime.now(), weight)
        );
    }
}
