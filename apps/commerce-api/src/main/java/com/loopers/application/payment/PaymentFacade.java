package com.loopers.application.payment;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Card;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final ProductService productService;
    private final CouponService couponService;

    @Transactional
    public PaymentInfo requestPayment(PaymentCommand.RequestPaymentCommand command) {
        Payment savedPayment = paymentService.getPaymentByOrderKey(command.orderKey());
        paymentService.validatePaymentStatusPending(savedPayment);

        Card card = paymentService.createCard(command.cardType(), command.cardNo());
        paymentService.applyCardInfo(savedPayment, card);

        User user = userService.getUserByLoginId(command.loginId());
        Order order = orderService.getOrderByOrderKey(command.orderKey());
        orderService.validateIsUserOrder(command.orderKey(), user.getId());

        try {
            Payment updatedPayment = paymentService.requestPaymentToPg(savedPayment, command.loginId());
            return PaymentInfo.from(updatedPayment);
        } catch (Exception e) {
            log.error("PG 결제 요청 실패: orderKey={}, error={}", command.orderKey(), e.getMessage(), e);
            handleFailure(savedPayment, order, "PG 결제 요청 실패: " + e.getMessage());
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 요청에 실패했습니다.");
        }
    }

    @Transactional
    public void handlePaymentCallback(PaymentV1Dto.PaymentCallbackRequest request) {
        Payment payment = paymentService.getPendingPaymentByOrderKey(request.orderId());
        Order order = orderService.getOrderByOrderKey(request.orderId());

        String status = request.status();
        switch (status) {
            case "SUCCESS" -> handleSuccess(payment, order);
            case "FAILED" -> handleFailure(payment, order, request.reason());
            default -> {
                log.warn("알 수 없는 결제 상태: transactionKey={}, status={}", request.transactionKey(), status);
            }
        }
    }

    @Transactional
    public void syncPaymentStatus(Payment payment) {
        Order order = orderService.getOrderByOrderKey(payment.getOrderKey());
        User user = userService.getUserById(order.getUserId());
        Payment updatedPayment = paymentService.checkPaymentStatusFromPg(payment, user.getLoginId().getId());

        if (updatedPayment.isCompleted()) {
            handleSuccess(updatedPayment, order);
        } else if (updatedPayment.isFailed()) {
            handleFailure(updatedPayment, order, "PG 상태 확인 결과 결제 실패");
        }
    }

    private void handleSuccess(Payment payment, Order order) {
        orderService.payOrder(order);
        log.info("결제 완료 처리: orderId={}, transactionKey={}", order.getId(), payment.getTransactionKey());
    }

    private void handleFailure(Payment payment, Order order, String reason) {
        orderService.cancelOrder(order);

        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productService.getProductById(orderItem.getProductId());
            productService.restoreStock(product, orderItem.getQuantity());
        }

        if (order.hasCoupon()) {
            Coupon coupon = couponService.getCouponByIdAndUserId(order.getCouponId(), order.getUserId());
            couponService.restoreCoupon(coupon);
        }

        log.warn("결제 실패 처리: orderKey={}, reason={}", payment.getOrderKey(), reason);
    }
}



