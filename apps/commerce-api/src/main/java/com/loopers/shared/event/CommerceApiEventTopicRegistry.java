package com.loopers.shared.event;

import com.loopers.shared.event.DomainEvent;
import com.loopers.shared.event.EventTopicRegistry;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 도메인 이벤트 타입과 Kafka 토픽 간의 매핑을 관리하는 레지스트리
 * 인프라 레이어에서 이벤트 타입에 따라 적절한 토픽을 결정합니다.
 * 
 * commerce-api 애플리케이션의 도메인 이벤트에 대한 토픽 매핑을 정의합니다.
 */
@Component
public class CommerceApiEventTopicRegistry implements EventTopicRegistry {
    
    private final Map<Class<? extends DomainEvent>, String> topicMap = new HashMap<>();
    
    public CommerceApiEventTopicRegistry() {
        // Order Events
        registerTopic(com.loopers.domain.order.event.OrderEvents.Created.class, "order.created.v1");
        registerTopic(com.loopers.domain.order.event.OrderEvents.Confirmed.class, "order.confirmed.v1");
        
        // Product Events
        registerTopic(com.loopers.domain.product.event.ProductEvents.Created.class, "product.created.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Updated.class, "product.updated.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Deleted.class, "product.deleted.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.LikeCount.class, "product.like-count-changed.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Viewed.class, "product.viewed.v1");
        
        // Stock Events
        registerTopic(com.loopers.domain.stock.event.StockEvents.Processed.class, "stock.deducted.v1");
        registerTopic(com.loopers.domain.stock.event.StockEvents.ProcessingFailed.class, "stock.deduction-failed.v1");
        registerTopic(com.loopers.domain.stock.event.StockEvents.Compensated.class, "stock.compensated.v1");
        
        // Coupon Events
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.Processed.class, "coupon.applied.v1");
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.ProcessingFailed.class, "coupon.apply-failed.v1");
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.Compensated.class, "coupon.compensated.v1");
        
        // Payment Events
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.CallbackReceived.class, "payment.callback-received.v1");
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.Processed.class, "payment.completed.v1");
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed.class, "payment.failed.v1");
        
        // Like Events
        registerTopic(com.loopers.domain.like.event.LikeEvents.ProductLikeSaved.class, "like.product-saved.v1");
        registerTopic(com.loopers.domain.like.event.LikeEvents.ProductLikeDeleted.class, "like.product-deleted.v1");
        registerTopic(com.loopers.domain.like.event.LikeEvents.LikeCountChanged.class, "like.count-changed.v1");
    }
    
    private void registerTopic(Class<? extends DomainEvent> eventClass, String topic) {
        topicMap.put(eventClass, topic);
    }
    
    @Override
    public String getTopic(Class<? extends DomainEvent> eventClass) {
        String topic = topicMap.get(eventClass);
        if (topic == null) {
            throw new IllegalArgumentException(
                "No topic registered for event type: " + eventClass.getName()
            );
        }
        return topic;
    }
}

