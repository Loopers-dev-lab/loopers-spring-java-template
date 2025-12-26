package com.loopers.application.event.listener;

import com.loopers.application.event.payment.PaymentCompletedEvent;
import com.loopers.application.event.order.OrderCompletedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusEventListener {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("[OrderStatusEventListener] 결제 완료 이벤트 처리 - orderNo: {}, status: {}",
                 event.orderNo(), event.status());

        Order order = orderRepository.findByOrderNo(event.orderNo())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                        "주문을 찾을 수 없습니다: " + event.orderNo()));

        if (event.isSuccess()) {
            // 이미 결제 완료된 경우 스킵 (멱등성)
            if (order.isPaid()) {
                log.debug("[OrderStatusEventListener] 이미 결제 완료된 주문 - orderNo: {}", event.orderNo());
                return;
            }

            order.markAsPaid();
            orderRepository.save(order);
            log.info("[OrderStatusEventListener] 주문 완료 처리 - orderNo: {}", event.orderNo());

            eventPublisher.publishEvent(new OrderCompletedEvent(
                order.getOrderNo(),
                order.getMemberId(),
                order.getTotalPrice(),
                order.getItems().stream()
                    .map(item -> new OrderCompletedEvent.OrderItemInfo(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                    ))
                    .toList(),
                java.time.LocalDateTime.now()
            ));

        } else if (event.isFailed()) {
            log.warn("[OrderStatusEventListener] 결제 실패 - orderNo: {}, reason: {}",
                     event.orderNo(), event.failureReason());

            // 결제 실패 시 주문 취소 및 재고 복구
            handlePaymentFailed(order, event.failureReason());
        }
    }

    /**
     * 결제 실패 처리
     * - 주문 취소
     * - 재고 복구
     * - 향후 알림 전송 가능
     */
    private void handlePaymentFailed(Order order, String failureReason) {
        // 이미 취소된 경우 스킵 (멱등성)
        if (order.isCancelled()) {
            log.debug("[OrderStatusEventListener] 이미 취소된 주문 - orderNo: {}", order.getOrderNo());
            return;
        }

        try {
            // 1. 주문 취소
            order.cancel();
            orderRepository.save(order);
            log.info("[OrderStatusEventListener] 주문 취소 완료 - orderNo: {}", order.getOrderNo());

            // 2. 재고 복구
            order.getItems().forEach(item -> {
                try {
                    productRepository.increaseStock(item.getProductId(), item.getQuantity());
                    log.debug("[OrderStatusEventListener] 재고 복구 - productId: {}, quantity: {}",
                            item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("[OrderStatusEventListener] 재고 복구 실패 - productId: {}, quantity: {}",
                            item.getProductId(), item.getQuantity(), e);
                    // 재고 복구 실패는 계속 진행 (다른 상품 복구)
                }
            });

            log.info("[OrderStatusEventListener] 결제 실패로 주문 취소 및 재고 복구 완료 - orderNo: {}, reason: {}",
                    order.getOrderNo(), failureReason);

            // TODO: 사용자에게 결제 실패 알림 전송
            // eventPublisher.publishEvent(new PaymentFailedNotificationEvent(
            //     order.getMemberId(), order.getOrderNo(), failureReason
            // ));

        } catch (Exception e) {
            log.error("[OrderStatusEventListener] 결제 실패 처리 중 오류 - orderNo: {}",
                    order.getOrderNo(), e);
            // TODO: 심각한 오류 알림 (수동 처리 필요)
        }
    }
}
