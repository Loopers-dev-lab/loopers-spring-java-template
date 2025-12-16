package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 보상 트랜잭션 서비스
 *
 * 결제 실패 시 주문 생성 과정에서 차감된 자원들을 복구합니다:
 * - 재고 복구
 * - 쿠폰 복구
 * - 포인트 환불 (선택적)
 * - 주문 취소
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompensationService {

    private final OrderService orderService;

    /**
     * 기본 주문 보상 트랜잭션
     *
     * 사용 시나리오: 포인트 결제 이벤트 처리 실패
     * - 재고 복구
     * - 쿠폰 복구
     * - 주문 취소
     *
     * @param orderId 보상 대상 주문
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateOrder(Long orderId) {
        Order order = orderService.getOrderWithDetailsById(orderId);

        // 멱등성 보장: 이미 취소된 주문은 스킵
        if (order.getStatus() == OrderStatus.CANCELED) {
            log.info("이미 취소된 주문입니다 - OrderId: {}", order.getId());
            return;
        }

        log.info("보상 트랜잭션 시작 - OrderId: {}", order.getId());

        // 1. 재고 복구
        order.getOrderItems().forEach(orderItem -> {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
            log.info("재고 복구 완료 - Product: {}, Quantity: {}",
                    orderItem.getProduct().getProductName(), orderItem.getQuantity());
        });

        // 2. 쿠폰 복구
        if (order.getIssuedCoupon() != null) {
            order.getIssuedCoupon().restoreCoupon();
            log.info("쿠폰 복구 완료 - IssuedCouponId: {}", order.getIssuedCoupon().getId());
        }

        // 3. 주문 취소
        order.cancelOrder();

        log.info("보상 트랜잭션 완료 - OrderId: {}", order.getId());
    }

    /**
     * 포인트 환불 포함 보상 트랜잭션
     *
     * 사용 시나리오: PG 결제 실패 콜백 처리
     * - 기본 보상 트랜잭션 실행
     * - 포인트 환불 추가
     *
     * @param orderId 보상 대상 주문
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensateOrderWithPointRefund(Long orderId) {
        Order order = orderService.getOrderWithDetailsById(orderId);

        // 멱등성 보장: 이미 취소된 주문은 스킵
        if (order.getStatus() == OrderStatus.CANCELED) {
            log.info("이미 취소된 주문입니다 - OrderId: {}", order.getId());
            return;
        }

        log.info("포인트 환불 포함 보상 트랜잭션 시작 - OrderId: {}", order.getId());

        // 1. 재고 복구
        order.getOrderItems().forEach(orderItem -> {
            orderItem.getProduct().increaseStock(orderItem.getQuantity());
            log.info("재고 복구 완료 - Product: {}, Quantity: {}",
                    orderItem.getProduct().getProductName(), orderItem.getQuantity());
        });

        // 2. 포인트 환불
        order.getUser().refundPoint(order.getTotalPrice());
        log.info("포인트 환불 완료 - UserId: {}, Amount: {}",
                order.getUser().getUserId(), order.getTotalPrice().getAmount());

        // 3. 쿠폰 복구
        if (order.getIssuedCoupon() != null) {
            order.getIssuedCoupon().restoreCoupon();
            log.info("쿠폰 복구 완료 - IssuedCouponId: {}", order.getIssuedCoupon().getId());
        }

        // 4. 주문 취소
        order.cancelOrder();

        log.info("포인트 환불 포함 보상 트랜잭션 완료 - OrderId: {}", order.getId());
    }
}
