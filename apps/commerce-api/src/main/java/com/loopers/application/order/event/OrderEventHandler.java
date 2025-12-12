package com.loopers.application.order.event;

import com.loopers.domain.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 주문 이벤트 핸들러
 * - 주문 생성 후 후속 처리를 비동기로 수행한다
 * - 쿠폰 사용 처리 (별도 트랜잭션)
 * - 데이터 플랫폼 전송 (로깅)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler {

    private final CouponService couponService;

    /**
     * 주문 생성 후 쿠폰 사용 처리
     * - 주문 트랜잭션이 커밋된 후 실행된다
     * - 별도 트랜잭션으로 실행되어 쿠폰 처리 실패가 주문에 영향을 주지 않는다
     * - 비동기로 실행되어 주문 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Async
    public void handleCouponUsage(OrderCreatedEvent event) {
        if (!event.hasCoupon()) {
            return;
        }

        try {
            log.info("[이벤트] 쿠폰 사용 처리 시작: orderId={}, couponId={}, userId={}",
                    event.orderId(), event.couponId(), event.userId());

            couponService.useCoupon(event.couponId(), event.userId());

            log.info("[이벤트] 쿠폰 사용 처리 완료: orderId={}, couponId={}", event.orderId(), event.couponId());
        } catch (Exception e) {
            // 쿠폰 사용 실패는 로그만 남기고 주문은 유지한다
            log.error("[이벤트] 쿠폰 사용 처리 실패: orderId={}, couponId={}, error={}",
                    event.orderId(), event.couponId(), e.getMessage(), e);
        }
    }

    /**
     * 주문 생성 후 데이터 플랫폼 전송
     * - 주문 트랜잭션이 커밋된 후 실행된다
     * - 비동기로 실행되어 주문 응답 속도에 영향을 주지 않는다
     * - 실패 시 로그만 남기고 재시도 로직은 별도로 구현 가능
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void sendToDataPlatform(OrderCreatedEvent event) {
        try {
            log.info("[이벤트] 데이터 플랫폼 전송: orderId={}, userId={}, amount={}",
                    event.orderId(), event.userId(), event.finalAmount());

            // TODO: 실제 데이터 플랫폼 API 호출
            // dataPlatformClient.sendOrder(event);

            log.info("[이벤트] 데이터 플랫폼 전송 완료: orderId={}", event.orderId());
        } catch (Exception e) {
            log.error("[이벤트] 데이터 플랫폼 전송 실패: orderId={}, error={}",
                    event.orderId(), e.getMessage(), e);
        }
    }

    /**
     * 주문 생성 시 사용자 행동 로깅
     * - 주문 생성 후 즉시 실행된다
     * - 비동기로 실행되어 주문 응답 속도에 영향을 주지 않는다
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void logUserAction(OrderCreatedEvent event) {
        try {
            log.info("[사용자 행동 로그] 주문 생성: userId={}, orderId={}, productCount={}, amount={}",
                    event.userId(), event.orderId(), event.orderItems().size(), event.finalAmount());

            // TODO: 실제 로깅 시스템 연동 (Elasticsearch, CloudWatch 등)
        } catch (Exception e) {
            log.error("[이벤트] 사용자 행동 로깅 실패: orderId={}, error={}",
                    event.orderId(), e.getMessage());
        }
    }
}
