package com.loopers.domain.coupon;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.order.OrderCreatedEventHandler;
import com.loopers.domain.order.OrderEvent;
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

@RequiredArgsConstructor
@Component
@Slf4j
public class CouponService implements OrderCreatedEventHandler {
    private final CouponRepository couponRepository;

    @Transactional
    public Coupon issuePercentage(Long userId, int discountPercent) {
        Coupon coupon = Coupon.issuePercentage(userId, discountPercent);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon issueFixed(Long userId, int discountAmount) {
        Coupon coupon = Coupon.issueFixed(userId, discountAmount);
        return couponRepository.save(coupon);
    }

    /**
     * 쿠폰 할인 계산 (사용 처리 없이 계산만)
     * 주문 생성 시 할인 금액 계산용
     */
    @Transactional(readOnly = true)
    public DiscountResult calculateDiscount(Long couponId, Long userId, Price totalAmount) {
        return couponRepository.findByIdAndUserId(couponId, userId)
                .map(c -> c.calculateDiscount(totalAmount))
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("eventTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderCreated(OrderEvent.OrderCreatedEvent event) {
        log.info("OrderCreatedEvent 수신. in {}.\nevent: {}", this.getClass().getSimpleName(), event);
        if (event == null) {
            log.warn("null event 수신. in {}. 무시합니다.", this.getClass().getSimpleName());
            return;
        }

        // 쿠폰 사용 처리 (쿠폰이 있는 경우)
        if (event.getCouponId() != null) {
            try {
                Coupon coupon = couponRepository.findByIdAndUserIdForUpdate(
                        event.getCouponId(),
                        event.getUserId()
                ).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

                // 쿠폰 사용 처리 (이미 할인 계산은 완료되었으므로 사용만 처리)
                coupon.markAsUsed();
                couponRepository.save(coupon);

                log.info("쿠폰 사용 처리 완료 - couponId: {}, orderId: {}", event.getCouponId(), event.getOrderId());
            } catch (Exception e) {
                log.error("쿠폰 사용 처리 실패 - couponId: {}, orderId: {}", event.getCouponId(), event.getOrderId(), e);
                // 쿠폰 사용 실패는 주문에 영향을 주지 않음 (이미 주문은 커밋됨)
                // todo: 이미 사용된 쿠폰인 경우 포인트 차감 등 별도 처리 고민 필요
            }
        }
    }
}
