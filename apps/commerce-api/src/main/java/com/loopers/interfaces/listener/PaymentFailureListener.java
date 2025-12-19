package com.loopers.interfaces.listener;

import com.loopers.application.payment.PaymentEvent;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFailureListener {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CouponService couponService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentEvent.PaymentFailed event) {
        log.info("PaymentFailed 이벤트 수신 - 주문 취소 및 복구 처리: orderKey={}, reason={}", event.orderKey(), event.reason());
        Order order = orderService.getOrderByOrderKey(event.orderKey());
        orderService.cancelOrder(order);

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productService.getProductById(orderItem.getProductId());
            productService.restoreStock(product, orderItem.getQuantity());
        }

        if (order.hasCoupon()) {
            Coupon coupon = couponService.getCouponByIdAndUserId(order.getCouponId(), order.getUserId());
            couponService.restoreCoupon(coupon);
        }

        Payment payment = paymentService.getPaymentByOrderKey(event.orderKey());
        payment.updateStatus(PaymentStatus.FAILED);
    }
}

