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
        // Order Events - consolidated to order.v1
        registerTopic(com.loopers.domain.order.event.OrderEvents.Created.class, "order.v1");
        registerTopic(com.loopers.domain.order.event.OrderEvents.Confirmed.class, "order.v1");
        
        // Product Events - consolidated to product.v1
        registerTopic(com.loopers.domain.product.event.ProductEvents.Created.class, "product.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Updated.class, "product.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Deleted.class, "product.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.LikeCount.class, "product.v1");
        registerTopic(com.loopers.domain.product.event.ProductEvents.Viewed.class, "product.v1");
        
        // Stock Events - consolidated to stock.v1
        registerTopic(com.loopers.domain.stock.event.StockEvents.Processed.class, "stock.v1");
        registerTopic(com.loopers.domain.stock.event.StockEvents.ProcessingFailed.class, "stock.v1");
        registerTopic(com.loopers.domain.stock.event.StockEvents.Compensated.class, "stock.v1");
        
        // Coupon Events - consolidated to coupon.v1
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.Processed.class, "coupon.v1");
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.ProcessingFailed.class, "coupon.v1");
        registerTopic(com.loopers.domain.coupon.event.CouponEvents.Compensated.class, "coupon.v1");
        
        // Payment Events - consolidated to payment.v1
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.CallbackReceived.class, "payment.v1");
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.Processed.class, "payment.v1");
        registerTopic(com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed.class, "payment.v1");
        
        // Like Events - consolidated to like.v1
        registerTopic(com.loopers.domain.like.event.LikeEvents.ProductLikeSaved.class, "like.v1");
        registerTopic(com.loopers.domain.like.event.LikeEvents.ProductLikeDeleted.class, "like.v1");
        registerTopic(com.loopers.domain.like.event.LikeEvents.LikeCountChanged.class, "like.v1");
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

