package com.loopers.application.payment;

import com.loopers.application.order.OrderFacade;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.infrastructure.PgClient;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
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
    private final PgClient pgClient;
    private final OrderFacade orderFacade;

    private static final int FIVE_MINUTES_IN_MS = 300000;
    private static final Long PENDING_ORDER_THRESHOLD = 5L;

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

    private void processOrders(List<Order> orders) {
        for (Order order : orders) {

            try {
                String transactionKey = order.getTransactionKey();
                PaymentV1Dto.PgPaymentResponse response;

                if (transactionKey == null) {
                    response = pgClient.getPaymentByOrderId(order.getId());
                } else {
                    response = pgClient.getPaymentByTransactionKey(transactionKey);
                }

                if (response.result() == PaymentV1Dto.pgPaymentStatus.SUCCESS) {
                    orderFacade.onPaymentSuccess(order.getId());
                } else {
                    orderFacade.onPaymentFailure(order.getId());
                }
            } catch (Exception e) {
                log.warn("결제 상태 동기화 중 오류 발생, orderId: {}", order.getId());
            }

        }
    }
}
