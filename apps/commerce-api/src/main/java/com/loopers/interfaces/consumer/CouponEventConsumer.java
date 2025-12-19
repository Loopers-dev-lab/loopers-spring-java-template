package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.event.CouponEventPublisher;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.stock.event.StockEvents;
import com.loopers.event.consumer.KafkaMessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"stock.deducted.v1", "payment.failed.v1"},
        groupId = "commerce-api-coupon-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class CouponEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final CouponService couponService;
    private final OrderService orderService;
    private final CouponEventPublisher couponEventPublisher;

    @KafkaHandler
    @Transactional
    public void handleStockProcessed(ConsumerRecord<String, StockEvents.Processed> record, Acknowledgment ack) {
        log.info("CouponEventConsumer: StockProcessedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "coupon.stock", event -> {
            var couponIds = event.originalEvent().couponIds();
            Long userId = event.originalEvent().userId();
            BigDecimal totalAmount = event.originalEvent().totalAmount();

            if (couponIds == null || couponIds.isEmpty()) {
                log.info("사용할 쿠폰 없음 - orderId: {}", event.orderId());
                couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(
                        event.orderId(),
                        userId,
                        BigDecimal.ZERO,
                        event
                ));
                return;
            }

            BigDecimal totalDiscountAmount = BigDecimal.ZERO;

            // 각 쿠폰에 대해 할인 금액 계산
            for (Long couponId : couponIds) {
                BigDecimal discount = couponService.useCoupon(
                        event.orderId(),
                        userId,
                        totalAmount,
                        couponId
                );
                totalDiscountAmount = totalDiscountAmount.add(discount);
            }

            // 주문에 할인 금액 반영
            if (totalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                orderService.applyDiscount(event.orderId(), totalDiscountAmount);
            }

            log.info("쿠폰 사용 성공 - orderId: {}, totalDiscountAmount: {}", event.orderId(), totalDiscountAmount);
            couponEventPublisher.publishCouponProcessed(new CouponEvents.Processed(
                    event.orderId(),
                    userId,
                    totalDiscountAmount,
                    event
            ));
        });
    }

    @KafkaHandler
    @Transactional
    public void handlePaymentProcessingFailed(ConsumerRecord<String, com.loopers.domain.payment.event.PaymentEvents.ProcessingFailed> record, Acknowledgment ack) {
        log.info("CouponEventConsumer: PaymentProcessingFailedEvent 수신 - orderId: {}", 
                record.value().orderId());

        messageProcessor.execute(record, ack, "coupon.payment", event -> {
            couponService.rollbackCoupon(event.orderId());
            log.info("쿠폰 원복 성공 - orderId: {}", event.orderId());
            couponEventPublisher.publishCouponCompensated(new CouponEvents.Compensated(event.orderId()));
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in coupon topics: {}", record.value());
        ack.acknowledge();
    }
}

