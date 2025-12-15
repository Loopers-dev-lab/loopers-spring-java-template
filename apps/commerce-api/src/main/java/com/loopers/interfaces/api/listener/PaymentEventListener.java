package com.loopers.interfaces.api.listener;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentSucceededEvent;
import com.loopers.domain.user.UserActionEvent;
import com.loopers.infrastructure.platform.DataPlatformSender;
import com.loopers.infrastructure.platform.OrderResultMessage;
import com.loopers.infrastructure.platform.PaymentResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderFacade orderFacade;
    private final DataPlatformSender dataPlatformSender;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 결제 성공 시 주문 완료 처리 + 데이터 플랫폼 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCompletion(PaymentSucceededEvent event) {
        log.info("결제 성공 후 주문 완료 처리: orderId={}, paymentId={}",
                event.orderId(), event.paymentId());

        try {
            orderFacade.completeOrder(event.orderId());

            // 주문 완료 데이터 플랫폼 전송
            OrderResultMessage orderMessage = OrderResultMessage.completed(event.orderId(), event.userId());
            dataPlatformSender.sendOrderResult(orderMessage);

            log.info("주문 완료 처리 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("주문 완료 처리 실패: orderId={}, reason={}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * 결제 성공 시 결제 데이터 플랫폼 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessDataPlatform(PaymentSucceededEvent event) {
        log.info("결제 성공 데이터 플랫폼 전송: paymentId={}", event.paymentId());

        try {
            PaymentResultMessage message = PaymentResultMessage.success(
                    event.paymentId(),
                    event.orderId(),
                    event.userId(),
                    event.amount(),
                    event.paymentMethod()
            );

            dataPlatformSender.sendPaymentResult(message);
        } catch (Exception e) {
            log.error("결제 성공 데이터 플랫폼 전송 실패: paymentId={}, reason={}",
                    event.paymentId(), e.getMessage());
        }
    }

    /**
     * 결제 성공 시 유저 행동 로깅
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessUserAction(PaymentSucceededEvent event) {
        log.debug("결제 성공 유저 행동 로깅: userId={}, paymentId={}", event.userId(), event.paymentId());

        eventPublisher.publishEvent(
                UserActionEvent.paymentSuccess(event.userId(), event.paymentId(), event.amount())
        );
    }

    /**
     * 결제 실패 시 보상 트랜잭션 (재고 복구, 쿠폰 복구, 주문 취소) + 데이터 플랫폼 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailedCompensation(PaymentFailedEvent event) {
        log.info("=== 결제 실패 보상 트랜잭션 시작: orderId={}, reason={} ===",
                event.orderId(), event.reason());

        try {
            orderFacade.cancelOrder(event.orderId(), event.couponId());

            // 주문 취소 데이터 플랫폼 전송
            OrderResultMessage orderMessage = OrderResultMessage.cancelled(event.orderId(), event.userId());
            dataPlatformSender.sendOrderResult(orderMessage);

            log.info("결제 실패 보상 트랜잭션 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("결제 실패 보상 트랜잭션 실패: orderId={}", event.orderId(), e);
        }
    }

    /**
     * 결제 실패 시 결제 데이터 플랫폼 전송
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailedDataPlatform(PaymentFailedEvent event) {
        log.info("결제 실패 데이터 플랫폼 전송: orderId={}", event.orderId());

        try {
            PaymentResultMessage message = PaymentResultMessage.failed(
                    null,
                    event.orderId(),
                    event.userId(),
                    null,
                    null,
                    null,
                    event.reason()
            );

            dataPlatformSender.sendPaymentResult(message);
        } catch (Exception e) {
            log.error("결제 실패 데이터 플랫폼 전송 실패: orderId={}, reason={}",
                    event.orderId(), e.getMessage());
        }
    }

    /**
     * 결제 실패 시 유저 행동 로깅
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailedUserAction(PaymentFailedEvent event) {
        log.debug("결제 실패 유저 행동 로깅: userId={}, orderId={}", event.userId(), event.orderId());

        eventPublisher.publishEvent(
                UserActionEvent.paymentFail(event.userId(), event.orderId(), event.reason())
        );
    }
}
