package com.loopers.application.payment.event;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 이벤트 핸들러
 * - 결제 완료 후 후속 처리를 비동기로 수행한다
 * - 주문 완료 처리 (별도 트랜잭션)
 * - 데이터 플랫폼 전송 (로깅)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final OrderRepository orderRepository;

    /**
     * 결제 완료 후 주문 완료 처리
     * - 결제 트랜잭션이 커밋된 후 실행된다
     * - 별도 트랜잭션으로 실행되어 주문 완료 처리 실패가 결제에 영향을 주지 않는다
     * - 결제 성공 시에만 주문을 완료 처리한다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void completeOrder(PaymentCompletedEvent event) {
        if (!event.isSuccess()) {
            log.info("[이벤트] 결제 실패로 주문 완료 처리 스킵: paymentId={}, orderId={}",
                    event.paymentId(), event.orderId());
            return;
        }

        try {
            log.info("[이벤트] 주문 완료 처리 시작: orderId={}, paymentId={}",
                    event.orderId(), event.paymentId());

            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: " + event.orderId()));

            order.complete();
            orderRepository.save(order);

            log.info("[이벤트] 주문 완료 처리 완료: orderId={}, paymentId={}", event.orderId(), event.paymentId());
        } catch (Exception e) {
            // 주문 완료 처리 실패는 로그만 남기고 결제는 유지한다
            // 스케줄러나 재시도 로직으로 복구 가능
            log.error("[이벤트] 주문 완료 처리 실패: orderId={}, paymentId={}, error={}",
                    event.orderId(), event.paymentId(), e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 후 데이터 플랫폼 전송
     * - 결제 트랜잭션이 커밋된 후 실행된다
     * - 비동기로 실행되어 결제 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void sendToDataPlatform(PaymentCompletedEvent event) {
        try {
            log.info("[이벤트] 결제 데이터 플랫폼 전송: paymentId={}, orderId={}, amount={}, status={}",
                    event.paymentId(), event.orderId(), event.amount(), event.status());

            // TODO: 실제 데이터 플랫폼 API 호출
            // dataPlatformClient.sendPayment(event);

            log.info("[이벤트] 결제 데이터 플랫폼 전송 완료: paymentId={}", event.paymentId());
        } catch (Exception e) {
            log.error("[이벤트] 결제 데이터 플랫폼 전송 실패: paymentId={}, error={}",
                    event.paymentId(), e.getMessage(), e);
        }
    }

    /**
     * 결제 완료 시 사용자 행동 로깅
     * - 결제 완료 후 즉시 실행된다
     * - 비동기로 실행되어 결제 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void logUserAction(PaymentCompletedEvent event) {
        try {
            log.info("[사용자 행동 로그] 결제 완료: userId={}, paymentId={}, orderId={}, amount={}, status={}",
                    event.userId(), event.paymentId(), event.orderId(), event.amount(), event.status());

            // TODO: 실제 로깅 시스템 연동 (Elasticsearch, CloudWatch 등)
        } catch (Exception e) {
            log.error("[이벤트] 사용자 행동 로깅 실패: paymentId={}, error={}",
                    event.paymentId(), e.getMessage());
        }
    }
}
