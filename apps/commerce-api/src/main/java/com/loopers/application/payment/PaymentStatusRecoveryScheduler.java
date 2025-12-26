package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.PaymentMethod;
import com.loopers.infrastructure.pg.PgPaymentExecutor;
import com.loopers.infrastructure.pg.PgV1Dto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Component
@Slf4j
public class PaymentStatusRecoveryScheduler {

    private static final int RECENT_THRESHOLD_MINUTES = 1;
    private static final int TIMEOUT_THRESHOLD_MINUTES = 30;

    private final OrderService orderService;
    private final PgPaymentExecutor pgPaymentExecutor;

    /**
     * 1분마다 실행되는 결제 상태 복구 작업
     */
    @Scheduled(fixedDelay = 60000)
    public void recoverPendingPayments() {
        ZonedDateTime recentThreshold = ZonedDateTime.now().minusMinutes(RECENT_THRESHOLD_MINUTES);
        ZonedDateTime timeoutThreshold = ZonedDateTime.now().minusMinutes(TIMEOUT_THRESHOLD_MINUTES);

        List<Order> pendingOrders = orderService.findPendingPaymentOrdersBefore(recentThreshold);

        log.info("결제 상태 복구 시작 - 대기 중인 주문: {}건", pendingOrders.size());

        for (Order order : pendingOrders) {
            if (order.getPaymentMethod() != PaymentMethod.PG) {
                continue; // 포인트 결제는 PG 복구 대상 아님
            }

            try {
                if (order.getPgTransactionKey() == null) {
                    handleOrderWithoutTransactionKey(order, timeoutThreshold);
                } else {
                    recoverOrderPaymentByTransactionKey(order);
                }
            } catch (Exception e) {
                log.error("결제 상태 복구 실패 - orderId: {}", order.getOrderId(), e);
            }
        }
    }

    /**
     * 트랜잭션 키가 없는 주문 처리
     *  - PG 시스템에 orderId로 조회
     *  - 기록이 있으면 상태 동기화
     *  - 기록이 없으면 생성 시간 기준으로 타임아웃/재시도/대기 처리
     */
    private void handleOrderWithoutTransactionKey(Order order, ZonedDateTime timeoutThreshold) {
        log.info("트랜잭션 키 없는 주문 처리 시작 - orderId: {}", order.getOrderId());

        // 1. PG 시스템에 orderId로 조회 시도
        PgV1Dto.PgOrderResponse response;
        try {
            response = pgPaymentExecutor.getPaymentByOrderIdAsync(order.getOrderId()).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("PG 시스템 조회 실패 - orderId: {}", order.getOrderId(), e);
            return;
        }

        if (BooleanUtils.isNotTrue(response.isFallback()) && !response.transactions().isEmpty()) {
            // PG 시스템에 기록이 있는 경우 (부분 성공 케이스)
            PgV1Dto.PgTransactionResponse latest = response.transactions().getFirst();
            syncOrderStatusFromPgResponse(order, latest);
            return;
        }

        // 2. PG 시스템에 기록이 없는 경우
        ZonedDateTime orderCreatedAt = order.getCreatedAt();

        if (orderCreatedAt.isBefore(timeoutThreshold)) {
            // 30분 이상 경과 → 실패 처리
            orderService.markAsFailedByTimeout(order.getId(), "결제 요청 타임아웃 - 트랜잭션 키 미발급");
            log.warn("주문 실패 처리 - orderId: {}, 결제 요청 타임아웃 - 트랜잭션 키 미발급", order.getOrderId());
        } else if (order.canRetryPayment()) {
            // 1분 이상 경과 & 재시도 가능 → 재결제 시도
            retryPaymentRequest(order);
        } else {
            // 재시도 불가 → 대기 (주기적 재조회만 수행)
            log.info("재결제 시도 불가 - orderId: {}, retryCount: {}",
                    order.getOrderId(), order.getRetryCount());
        }
    }

    private void retryPaymentRequest(Order order) {
        log.info("재결제 시도 시작 - orderId: {}", order.getOrderId());
        PgV1Dto.PgPaymentRequest request = buildPaymentRequestFromOrder(order);

        pgPaymentExecutor.requestPaymentAsync(request)
                .thenAccept(response -> {
                    if (response.transactionKey() != null) {
                        // 재결제 성공
                        // PgV1Dto를 도메인 VO로 변환
                        com.loopers.domain.order.vo.PgTransactionResponse domainResponse = 
                                new com.loopers.domain.order.vo.PgTransactionResponse(
                                        response.transactionKey(),
                                        response.status(),
                                        response.reason()
                                );
                        orderService.updateStatusByPgResponse(order.getId(), domainResponse);
                        log.info("재결제 성공 - orderId: {}, transactionKey: {}",
                                order.getOrderId(), response.transactionKey());
                    } else {
                        // 재결제 실패 (Fallback)
                        orderService.incrementRetryCount(order.getId());
                        log.warn("재결제 실패 (Fallback) - orderId: {}", order.getOrderId());
                    }
                })
                .exceptionally(e -> {
                    orderService.incrementRetryCount(order.getId());
                    log.error("재결제 시도 중 예외 발생 - orderId: {}", order.getOrderId(), e);
                    return null;
                });
    }

    /**
     * Order로부터 PG 결제 요청 객체 생성
     */
    private PgV1Dto.PgPaymentRequest buildPaymentRequestFromOrder(Order order) {
        return new PgV1Dto.PgPaymentRequest(
                order.getOrderId(),
                order.getCardType(),
                order.getCardNo(),
                (long) order.getFinalPrice().amount(),
                order.getCallbackUrl()
        );
    }

    /**
     * PG 응답으로 주문 상태 동기화
     */
    private void syncOrderStatusFromPgResponse(Order order, PgV1Dto.PgTransactionResponse response) {
        // PgV1Dto를 도메인 VO로 변환
        com.loopers.domain.order.vo.PgTransactionResponse domainResponse = 
                new com.loopers.domain.order.vo.PgTransactionResponse(
                        response.transactionKey(),
                        response.status(),
                        response.reason()
                );
        orderService.updateStatusByPgResponse(order.getId(), domainResponse);
        log.info("PG 상태 동기화 성공 - orderId: {}, transactionKey: {}, status: {}",
                order.getOrderId(), response.transactionKey(), response.status());
    }

    /**
     * 트랜잭션 키가 있는 주문의 상태 복구
     */
    private void recoverOrderPaymentByTransactionKey(Order order) {
        log.info("트랜잭션 키로 상태 복구 시작 - orderId: {}, transactionKey: {}",
                order.getOrderId(), order.getPgTransactionKey());

        pgPaymentExecutor.getPaymentDetailByTransactionKeyAsync(order.getPgTransactionKey())
                .thenAccept(transactionDetail -> {
                    // PgTransactionDetailResponse를 PgV1Dto로 변환 후 도메인 VO로 변환
                    PgV1Dto.PgTransactionResponse pgResponse = new PgV1Dto.PgTransactionResponse(
                            transactionDetail.transactionKey(),
                            transactionDetail.status(),
                            transactionDetail.pgPaymentReason()
                    );
                    syncOrderStatusFromPgResponse(order, pgResponse);
                })
                .exceptionally(e -> {
                    log.warn("결제 상태 복구 실패 - orderId: {}, transactionKey: {}",
                            order.getOrderId(), order.getPgTransactionKey(), e);
                    return null;
                });
    }
}
