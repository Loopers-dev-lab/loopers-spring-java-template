package com.loopers.infrastructure.logging;

import com.loopers.domain.coupon.event.CouponUsedEvent;
import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.order.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class UserActionLoggingHandler {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("[USER_ACTION] ORDER_COMPLETED userId={}, orderId={}, totalAmount={}, paymentAmount={}",
                event.getUserId(), event.getOrderId(), event.getTotalAmount(), event.getPaymentAmount());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductLiked(ProductLikedEvent event) {
        String action = event.isLiked() ? "PRODUCT_LIKED" : "PRODUCT_UNLIKED";
        log.info("[USER_ACTION] {} userId={}, productId={}",
                action, event.getUserId(), event.getProductId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponUsed(CouponUsedEvent event) {
        log.info("[USER_ACTION] COUPON_USED userId={}, couponId={}, orderId={}, discountAmount={}",
                event.getUserId(), event.getCouponId(), event.getOrderId(), event.getDiscountAmount());
    }
}
