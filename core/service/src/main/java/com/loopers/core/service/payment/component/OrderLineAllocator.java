package com.loopers.core.service.payment.component;

import com.loopers.JacksonUtil;
import com.loopers.core.domain.event.EventOutbox;
import com.loopers.core.domain.event.repository.EventOutboxRepository;
import com.loopers.core.domain.event.type.AggregateType;
import com.loopers.core.domain.event.type.EventType;
import com.loopers.core.domain.event.vo.EventId;
import com.loopers.core.domain.event.vo.EventPayload;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.event.ProductOutOfStockEvent;
import com.loopers.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderLineAllocator {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final EventOutboxRepository eventOutboxRepository;

    @Transactional
    public BigDecimal allocate(OrderItem orderItem) {
        Product product = productRepository.getByIdWithLock(orderItem.getProductId());
        Product decreased = product.decreaseStock(orderItem.getQuantity());
        productRepository.save(decreased);
        orderItemRepository.save(orderItem);

        if (decreased.outOfStock()) {
            EventId eventId = EventId.generate();
            eventOutboxRepository.save(
                    EventOutbox.create(
                            eventId,
                            AggregateType.PRODUCT,
                            product.getId().toAggregateId(),
                            EventType.OUT_OF_STOCK,
                            new EventPayload(
                                    JacksonUtil.convertToString(new ProductOutOfStockEvent(eventId, product.getId()))
                            )
                    )
            );
        }

        return product.getTotalPrice(orderItem.getQuantity());
    }
}
