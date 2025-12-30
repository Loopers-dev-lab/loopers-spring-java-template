package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.event.InboxEventService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.strategy.PaymentStrategy;
import com.loopers.domain.payment.strategy.PaymentStrategyFactory;
import com.loopers.infrastructure.payment.event.PaymentInboxEventRepository;
import com.loopers.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 결제 관련 이벤트 핸들러
 * SAGA 패턴의 결제 처리 로직
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final OrderService orderService;
    private final PaymentInboxEventRepository paymentInboxEventRepository;
    private final InboxEventService inboxEventService;

    @Transactional(noRollbackFor = CoreException.class)
    public void handlePaymentCallbackReceived(PaymentEvents.CallbackReceived event) {
        log.info("PaymentEventHandler: PaymentCallbackReceivedEvent 처리 - orderId: {}, transactionKey: {}, status: {}",
                event.orderId(), event.transactionKey(), event.status());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                paymentInboxEventRepository,
                event,
                "payment.v1",
                (eventId, aggregateId, type, topic) -> PaymentInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

        try {
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

                // CommercePayment와 Order를 조회하여 userId와 finalAmount 획득
                CommercePayment commercePayment = paymentService.findByTransactionKey(event.transactionKey());
                var order = orderService.findOrderById(event.orderId());
                
                Long userId = order.getUserId();
                BigDecimal finalAmount = commercePayment.getAmount();

                // 결제 성공 이벤트 발행
                paymentEventPublisher.publishPaymentProcessed(
                        new PaymentEvents.Processed(
                                event.orderId(),
                                userId,
                                finalAmount,
                                null   // PG 콜백 경로이므로 originalEvent는 null
                        )
                );
            }
        } catch (Exception e) {
            // 콜백 처리 중 예외 발생 시 실패 이벤트 발행
            String failureReason = e.getMessage() != null ? e.getMessage() : "결제 콜백 처리 중 알 수 없는 오류 발생";
            log.error("결제 콜백 처리 실패 - orderId: {}, transactionKey: {}, reason: {}",
                    event.orderId(), event.transactionKey(), failureReason, e);

            paymentEventPublisher.publishPaymentProcessingFailed(
                    new PaymentEvents.ProcessingFailed(
                            event.orderId(),
                            null,
                            failureReason
                    )
            );
            // 예외를 다시 던지지 않고 return하여 트랜잭션이 커밋되어 실패 이벤트가 발행되도록 함
            return;
        }
    }

    @Transactional(noRollbackFor = CoreException.class)
    public void handleCouponProcessed(CouponEvents.Processed event) {
        log.info("PaymentEventHandler: CouponProcessedEvent 처리 - orderId: {}", event.orderId());

        // Inbox 패턴을 통한 멱등성 체크
        boolean isDuplicate = inboxEventService.checkAndSave(
                paymentInboxEventRepository,
                event,
                "coupon.v1",
                (eventId, aggregateId, type, topic) -> PaymentInboxEvent.builder()
                        .eventId(eventId)
                        .aggregateId(aggregateId)
                        .type(type)
                        .topic(topic)
                        .build()
        );
        if (isDuplicate) {
            log.info("Duplicate event detected in Inbox, skipping - eventId: {}, orderId: {}", 
                    event.getEventId(), event.orderId());
            return;
        }

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
        try {
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
        } catch (Exception e) { // Catch any exception during payment processing or saving
            String failureReason = e.getMessage() != null ? e.getMessage() : "결제 처리 중 알 수 없는 오류 발생";
            log.error("결제 처리 실패 - orderId: {}, method: {}, reason: {}",
                    event.orderId(), strategy.getPaymentMethod(), failureReason, e);

            paymentEventPublisher.publishPaymentProcessingFailed(new PaymentEvents.ProcessingFailed(
                    event.orderId(),
                    event,
                    failureReason
            ));
            return; // Allow transaction to commit with the published failure event
        }
    }
}

