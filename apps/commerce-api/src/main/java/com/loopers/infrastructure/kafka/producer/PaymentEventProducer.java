package com.loopers.infrastructure.kafka.producer;

import com.loopers.infrastructure.kafka.dto.PaymentEventDto;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${kafka.topic.payment-events-name}")
    private String paymentEventsTopic;

    @Retry(name = "kafkaProducer", fallbackMethod = "paymentSuccessFallback")
    public void sendPaymentSuccessEvent(Long orderId, Long userId, String transactionId, Long amount) {
        PaymentEventDto event = PaymentEventDto.success(orderId, userId, transactionId, amount);
        kafkaTemplate.send(paymentEventsTopic, orderId.toString(), event);
        log.info("결제 성공 이벤트 발행: orderId={}, transactionId={}", orderId, transactionId);
    }

    @Retry(name = "kafkaProducer", fallbackMethod = "paymentFailedFallback")
    public void sendPaymentFailedEvent(Long orderId, Long userId, String failureReason) {
        PaymentEventDto event = PaymentEventDto.failed(orderId, userId, failureReason);
        kafkaTemplate.send(paymentEventsTopic, orderId.toString(), event);
        log.info("결제 실패 이벤트 발행: orderId={}, reason={}", orderId, failureReason);
    }

    @Retry(name = "kafkaProducer", fallbackMethod = "paymentPendingFallback")
    public void sendPaymentPendingEvent(Long orderId, Long userId, String transactionId) {
        PaymentEventDto event = PaymentEventDto.pending(orderId, userId, transactionId);
        kafkaTemplate.send(paymentEventsTopic, orderId.toString(), event);
        log.info("결제 대기 이벤트 발행: orderId={}, transactionId={}", orderId, transactionId);
    }

    public void paymentSuccessFallback(Long orderId, Long userId, String transactionId, Long amount, Throwable ex) {
        log.error("결제 성공 이벤트 발행 실패: orderId={}", orderId, ex);
    }

    public void paymentFailedFallback(Long orderId, Long userId, String failureReason, Throwable ex) {
        log.error("결제 실패 이벤트 발행 실패: orderId={}", orderId, ex);
    }

    public void paymentPendingFallback(Long orderId, Long userId, String transactionId, Throwable ex) {
        log.error("결제 대기 이벤트 발행 실패: orderId={}", orderId, ex);
    }
}
