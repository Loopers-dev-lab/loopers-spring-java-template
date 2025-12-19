package com.loopers.event.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.coupon.event.CouponOutboxEvent;
import com.loopers.domain.coupon.event.CouponOutboxEventRepository;
import com.loopers.domain.like.event.LikeOutboxEvent;
import com.loopers.domain.like.event.LikeOutboxEventRepository;
import com.loopers.domain.order.event.OrderOutboxEvent;
import com.loopers.domain.order.event.OrderOutboxEventRepository;
import com.loopers.domain.payment.event.PaymentOutboxEvent;
import com.loopers.domain.payment.event.PaymentOutboxEventRepository;
import com.loopers.domain.product.event.ProductOutboxEvent;
import com.loopers.domain.product.event.ProductOutboxEventRepository;
import com.loopers.domain.stock.event.StockOutboxEvent;
import com.loopers.domain.stock.event.StockOutboxEventRepository;
import com.loopers.event.outbox.OutboxEventPublisher;
import com.loopers.shared.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalOutboxEventPublisher implements OutboxEventPublisher {

    private final OrderOutboxEventRepository orderOutboxEventRepository;
    private final StockOutboxEventRepository stockOutboxEventRepository;
    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final CouponOutboxEventRepository couponOutboxEventRepository;
    private final ProductOutboxEventRepository productOutboxEventRepository;
    private final LikeOutboxEventRepository likeOutboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String topic, String key, DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String type = event.getClass().getSimpleName();
            String aggregateType = event.getAggregateType();

            // aggregateType에 따라 적절한 Repository에 저장
            saveToOutbox(aggregateType, event.getEventId(), key, type, payload, topic);
            
            log.debug("Saved event to Outbox - aggregateType: {}, eventId: {}, topic: {}, key: {}, type: {}", 
                    aggregateType, event.getEventId(), topic, key, type);
            
        } catch (Exception e) {
            log.error("Failed to save event to Outbox - eventId: {}, aggregateType: {}", 
                    event.getEventId(), event.getAggregateType(), e);
            throw new RuntimeException("Failed to save event to Outbox", e);
        }
    }

    private void saveToOutbox(String aggregateType, String eventId, String aggregateId, 
                              String type, String payload, String topic) {
        switch (aggregateType) {
            case "ORDER" -> {
                OrderOutboxEvent outboxEvent = OrderOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                orderOutboxEventRepository.save(outboxEvent);
            }
            case "STOCK" -> {
                StockOutboxEvent outboxEvent = StockOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                stockOutboxEventRepository.save(outboxEvent);
            }
            case "PAYMENT" -> {
                PaymentOutboxEvent outboxEvent = PaymentOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                paymentOutboxEventRepository.save(outboxEvent);
            }
            case "COUPON" -> {
                CouponOutboxEvent outboxEvent = CouponOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                couponOutboxEventRepository.save(outboxEvent);
            }
            case "PRODUCT" -> {
                ProductOutboxEvent outboxEvent = ProductOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                productOutboxEventRepository.save(outboxEvent);
            }
            case "LIKE" -> {
                LikeOutboxEvent outboxEvent = LikeOutboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .payload(payload)
                        .topic(topic)
                        .build();
                likeOutboxEventRepository.save(outboxEvent);
            }
            default -> throw new IllegalArgumentException("Unknown aggregate type: " + aggregateType);
        }
    }
}

