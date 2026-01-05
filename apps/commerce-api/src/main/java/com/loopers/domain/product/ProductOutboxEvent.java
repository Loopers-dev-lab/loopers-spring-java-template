package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "product_outbox_events")
@Entity
public class ProductOutboxEvent extends BaseEntity {

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    private ProductOutboxStatus productOutboxStatus;

    private Long productId;

    public static ProductOutboxEvent create(final EventType eventType, final Long productId) {
        ProductOutboxEvent productOutboxEvent = new ProductOutboxEvent();
        productOutboxEvent.eventType = eventType;
        productOutboxEvent.productOutboxStatus = ProductOutboxStatus.PENDING;
        productOutboxEvent.productId = productId;
        return productOutboxEvent;
    }
}

