package com.loopers.application.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.PgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PaymentHistoryResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dto.PgResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 결제 상태 동기화 스케줄러.
 * <p>
 * PG(결제 대행사)로부터 콜백을 받지 못했거나 타임아웃 등으로 인해 상태가 불분명해진 주문에 대해,
 * 주기적으로 PG사에 실제 결제 상태를 조회하여 우리 시스템의 주문 상태를 최종적으로 일치시키는 역할을 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSyncScheduler {

    private final OrderService orderService;
    private final OrderFacade orderFacade;
    private final PaymentFacade paymentFacade;
    private final PaymentService paymentService;

    private final PgClient pgClient;

    private enum PaymentStatus {
        SUCCESS,
        FAILURE,
        PENDING
    }

    private static final int FIVE_MINUTES_IN_MS = 300000;
    private static final Long PENDING_ORDER_THRESHOLD = 5L;
    private static final String SUCCESS_CODE = "SUCCESS";

    @Scheduled(fixedRate = FIVE_MINUTES_IN_MS)
    public void synchronizePendingPayments() {

        List<Order> orders = orderService.findPendingOrders(PENDING_ORDER_THRESHOLD);

        if (orders == null || orders.isEmpty()) {
            return;
        }

        log.info("주문 총 {} 건 동기화 시작", orders.size());
        processOrders(orders);
        log.info("결제 상태 동기화 종료");
    }

    private PaymentStatus getPaymentStatus(String status) {
        switch (status) {
            case "SUCCESS" -> { return PaymentStatus.SUCCESS; }
            case "PENDING" -> { return PaymentStatus.PENDING; }
            default -> { return PaymentStatus.FAILURE; }
        }
    }

    private void processOrders(List<Order> orders) {
        for (Order order : orders) {
            try {
                Payment payment = paymentService.getPaymentByOrderId(order.getId());
                PaymentStatus paymentStatus = PaymentStatus.PENDING;

                // 멱등키를 통한 결제 상태 조회
                if (payment.getIdempotencyKey() != null) {

                    PgResponse<PaymentResponse> response = pgClient.getPaymentByTransactionKey(payment.getIdempotencyKey());

                    if (SUCCESS_CODE.equals(response.meta().result()) && response.data() != null) {
                        paymentStatus = getPaymentStatus(response.data().status());
                    } else {
                        log.warn("PG사 결제상태 조회 실패, orderId: {}, pgResponse: {}", payment.getOrderId(), response);
                    }
                } else {
                    // 주문아이디를 통해 결제 시도 내역 전체 조회
                    PgResponse<PaymentHistoryResponse> response = pgClient.getPaymentByOrderId(order.getId());

                    if (SUCCESS_CODE.equals(response.meta().result()) && response.data() != null && response.data().transactions() != null) {
                        // 트랜잭션 리스트에서 하나라도 성공한 것이 있는지 확인
                        boolean hasSuccessfulTx = response.data().transactions().stream()
                                .anyMatch(tx -> SUCCESS_CODE.equals(tx.status()));

                        if (hasSuccessfulTx) {
                            paymentStatus = PaymentStatus.SUCCESS;
                        } else {
                            paymentStatus = PaymentStatus.FAILURE;
                        }
                    } else {
                        log.warn("PG사 결제상태 조회 실패, orderId: {}, pgResponse: {}", payment.getOrderId(), response);
                    }
                }

                // 결과에 따라 OrderFacade 호출
                switch (paymentStatus) {
                    case SUCCESS -> orderFacade.handleOrderSucceed(order.getId());
                    case FAILURE -> orderFacade.handleOrderFailure(order.getUserId(), order.getId());
                    default -> log.info("주문의 결제 상태가 대기 중이거나 알 수 없습니다, orderId: {}", order.getId());
                }

            } catch (Exception e) {
                log.warn("결제 상태 동기화 중 오류 발생, orderId: {}, 에러: {}", order.getId(), e.getMessage(), e);
            }
        }
    }
}
