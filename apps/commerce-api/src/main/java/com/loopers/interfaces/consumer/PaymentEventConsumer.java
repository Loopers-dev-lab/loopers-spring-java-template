package com.loopers.interfaces.consumer;

import com.loopers.config.kafka.KafkaConfig;
import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.event.PaymentEventPublisher;
import com.loopers.domain.payment.event.PaymentEvents;
import com.loopers.domain.payment.strategy.PaymentStrategy;
import com.loopers.domain.payment.strategy.PaymentStrategyFactory;
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
        topics = {"payment.callback-received.v1", "coupon.applied.v1"},
        groupId = "commerce-api-payment-group",
        containerFactory = KafkaConfig.SINGLE_LISTENER
)
public class PaymentEventConsumer {

    private final KafkaMessageProcessor messageProcessor;
    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @KafkaHandler
    @Transactional
    public void handlePaymentCallbackReceived(ConsumerRecord<String, PaymentEvents.CallbackReceived> record, Acknowledgment ack) {
        log.info("PaymentEventConsumer: PaymentCallbackReceivedEvent 수신 - orderId: {}, transactionKey: {}, status: {}",
                record.value().orderId(), record.value().transactionKey(), record.value().status());

        messageProcessor.execute(record, ack, "payment.callback", event -> {

            // 결제 상태에 따라 처리
            if (event.status() == PaymentDto.PaymentStatus.FAILED) {
                // 결제 실패 처리
                paymentService.saveFailedPayment(event.transactionKey(), event.reason());
                log.info("결제 실패 처리 완료 - orderId: {}, transactionKey: {}, reason: {}",
                        event.orderId(), event.transactionKey(), event.reason());

                // 주문 보상 이벤트 발행
                paymentEventPublisher.publishPaymentProcessingFailed(
                        new PaymentEvents.ProcessingFailed(
                                event.orderId(),
                                null,  // PG 콜백 경로에서는 originalEvent 없음
                                event.reason()
                        )
                );
            } else {
                // 결제 성공 처리
                paymentService.saveSuccessPayment(event.transactionKey());
                log.info("결제 성공 처리 완료 - orderId: {}, transactionKey: {}",
                        event.orderId(), event.transactionKey());

                // 결제 성공 이벤트 발행
                // PG 콜백 경로에서는 userId와 finalAmount를 알 수 없으므로,
                // CommercePayment에서 조회하거나 별도 처리 필요
                // 일단 기본값으로 처리 (실제로는 CommercePayment 조회 필요)
                paymentEventPublisher.publishPaymentProcessed(
                        new PaymentEvents.Processed(
                                event.orderId(),
                                null,  // userId는 CommercePayment에서 조회 필요
                                null,  // finalAmount는 CommercePayment에서 조회 필요
                                null   // PG 콜백 경로이므로 originalEvent는 null
                        )
                );
            }
        });
    }

    @KafkaHandler
    @Transactional
    public void handleCouponProcessed(ConsumerRecord<String, CouponEvents.Processed> record, Acknowledgment ack) {
        log.info("PaymentEventConsumer: CouponProcessedEvent 수신 - orderId: {}",
                record.value().orderId());

        messageProcessor.execute(record, ack, "payment.coupon", event -> {

            // 이벤트에서 필요한 데이터 가져오기
            Long userId = event.userId();
            BigDecimal totalPrice = event.originalEvent().originalEvent().totalAmount();
            BigDecimal totalDiscountAmount = event.totalDiscountAmount();

            // 최종 결제 금액 계산 (할인 금액 제외)
            BigDecimal finalAmount = totalPrice.subtract(totalDiscountAmount);

            // 결제 금액이 0 이하이면 결제 처리 불필요
            if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("결제 금액이 0원 이하이므로 결제 처리 생략 - orderId: {}", event.orderId());
                paymentEventPublisher.publishPaymentProcessed(new PaymentEvents.Processed(
                        event.orderId(),
                        userId,
                        finalAmount,
                        event
                ));
                return;
            }

            // 결제 방법 추출 (이벤트 체인을 통해 전달된 paymentMethod에서 가져옴)
            PaymentDto.PaymentMethod paymentMethod = event.originalEvent()
                    .originalEvent()
                    .paymentMethod();

            // 결제 전략 선택 및 처리 (Order 엔티티 없이 필요한 값만 전달)
            PaymentStrategy strategy = paymentStrategyFactory.getStrategy(paymentMethod);
            PaymentStrategy.PaymentResult result = strategy.processPayment(event.orderId(), userId, finalAmount);

            // 결제 결과에 따른 처리
            if (result.success()) {
                // CommercePayment 저장
                CommercePayment.CommercePaymentBuilder paymentBuilder = CommercePayment.builder()
                        .orderId(event.orderId())
                        .transactionKey(result.transactionKey())
                        .method(strategy.getPaymentMethod())
                        .paymentStatus(result.status())
                        .amount(finalAmount);

                // 카드 결제인 경우에만 카드 정보 저장
                if (strategy.getPaymentMethod() == PaymentDto.PaymentMethod.CARD) {
                    paymentBuilder.cardType(PaymentDto.CardType.SAMSUNG)
                            .cardNo("1111-2222-3333-4444");
                }

                paymentService.saveCommercePayment(paymentBuilder.build());

                log.info("결제 처리 성공 - orderId: {}, method: {}, status: {}",
                        event.orderId(), strategy.getPaymentMethod(), result.status());

                // 결제 성공 이벤트 발행
                paymentEventPublisher.publishPaymentProcessed(new PaymentEvents.Processed(
                        event.orderId(),
                        userId,
                        finalAmount,
                        event
                ));
            } else {
                // 결제 실패 처리
                String failureReason = result.reason() != null ? result.reason() : "결제 요청에 실패했습니다.";
                log.error("결제 처리 실패 - orderId: {}, method: {}, reason: {}",
                        event.orderId(), strategy.getPaymentMethod(), failureReason);

                paymentEventPublisher.publishPaymentProcessingFailed(new PaymentEvents.ProcessingFailed(
                        event.orderId(),
                        event,  // 재고 원복을 위해 포함
                        failureReason
                ));
            }
        });
    }

    @KafkaHandler(isDefault = true)
    public void handleDefault(ConsumerRecord<Object, Object> record, Acknowledgment ack) {
        log.warn("Received unknown event in payment topics: {}", record.value());
        ack.acknowledge();
    }
}

