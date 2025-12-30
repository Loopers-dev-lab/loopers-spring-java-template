package com.loopers.interfaces.consumer;

import com.loopers.application.kafka.KafkaEvent;
import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.cache.ProductCacheService;
import com.loopers.domain.eventhandled.EventHandledService;
import com.loopers.domain.metrics.ProductMetricsService;
import com.loopers.domain.ranking.RankingService;
import com.loopers.domain.ranking.RankingWeight;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MetricsConsumer {

    private final EventHandledService eventHandledService;
    private final ProductMetricsService productMetricsService;
    private final ProductCacheService productCacheService;
    private final RankingService rankingService;

    @KafkaListener(
            topics = {"product-liked-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleProductLikedEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.ProductEvent.ProductLiked event = (KafkaEvent.ProductEvent.ProductLiked) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            productMetricsService.incrementLikeCount(event.productId());
            rankingService.incrementScore(event.productId(), RankingWeight.LIKE);
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "ProductLiked",
                    event.productId().toString()
            );
            log.info("상품 좋아요 수 집계 완료: productId={}", event.productId());
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = {"product-unliked-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleProductUnlikedEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.ProductEvent.ProductUnliked event = (KafkaEvent.ProductEvent.ProductUnliked) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            productMetricsService.decrementLikeCount(event.productId());
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "ProductUnliked",
                    event.productId().toString()
            );
            log.info("상품 좋아요 취소 수 집계 완료: productId={}", event.productId());
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = {"product-viewed-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleProductViewedEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.ProductEvent.ProductViewed event = (KafkaEvent.ProductEvent.ProductViewed) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            productMetricsService.incrementViewCount(event.productId());
            rankingService.incrementScore(event.productId(), RankingWeight.VIEW);
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "ProductViewed",
                    event.productId().toString()
            );
            log.info("상품 조회 수 집계 완료: productId={}", event.productId());
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = {"order-created-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleOrderCreatedEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.OrderEvent.OrderCreated event = (KafkaEvent.OrderEvent.OrderCreated) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            log.info("주문 생성 이벤트 수신: orderId={}, orderKey={}", event.orderId(), event.orderKey());
            
            for (KafkaEvent.OrderEvent.OrderItemInfo orderItem : event.orderItems()) {
                rankingService.incrementScore(orderItem.productId(), RankingWeight.ORDER_CREATED);
            }
            
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "OrderCreated",
                    event.orderKey()
            );
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = {"order-paid-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleOrderPaidEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.OrderEvent.OrderPaid event = (KafkaEvent.OrderEvent.OrderPaid) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            log.info("주문 결제 이벤트 수신: orderId={}, orderKey={}, orderItems={}", 
                    event.orderId(), event.orderKey(), event.orderItems().size());
            
            for (KafkaEvent.OrderEvent.OrderItemInfo orderItem : event.orderItems()) {
                productMetricsService.incrementSalesCount(orderItem.productId(), orderItem.quantity());
            }
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "OrderPaid",
                    event.orderKey()
            );
        }

        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = {"product-stock-depleted-events"},
            containerFactory = KafkaConfig.BATCH_LISTENER,
            groupId = "metrics-consumer-group"
    )
    @Transactional
    public void handleProductStockDepletedEvents(
            List<ConsumerRecord<String, Object>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, Object> record : messages) {
            KafkaEvent.ProductEvent.ProductStockDepleted event = (KafkaEvent.ProductEvent.ProductStockDepleted) record.value();

            if (eventHandledService.isAlreadyHandled(event.eventId())) {
                continue;
            }

            productCacheService.invalidateProductCache(event.productId());
            eventHandledService.markAsHandled(
                    event.eventId(),
                    "ProductStockDepleted",
                    event.productId().toString()
            );
            log.info("재고 소진으로 인한 상품 캐시 삭제 완료: productId={}, remainingStock={}", 
                    event.productId(), event.remainingStock());
        }

        acknowledgment.acknowledge();
    }
}
