package com.loopers.application.payment;

import com.loopers.application.userbehavior.UserBehaviorEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Card;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.payment.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentInfo requestPayment(PaymentCommand.RequestPaymentCommand command) {
        Payment payment = paymentService.getPaymentByOrderKey(command.orderKey());
        paymentService.validatePaymentStatusPending(payment);

        Card card = paymentService.createCard(command.cardType(), command.cardNo());
        paymentService.applyCardInfo(payment, card);

        User user = userService.getUserByLoginId(command.loginId());
        orderService.validateIsUserOrder(command.orderKey(), user.getId());

        try {
            paymentService.requestPaymentToPg(payment, command.loginId());

            eventPublisher.publishEvent(
                    UserBehaviorEvent.PaymentRequested.from(
                            user.getId(),
                            command.orderKey(),
                            payment.getAmount()
                    )
            );
            
            return PaymentInfo.from(payment);
        } catch (Exception e) {
            log.error("PG 결제 요청 실패: orderKey={}, error={}", command.orderKey(), e.getMessage(), e);
            eventPublisher.publishEvent(PaymentEvent.PaymentStatusUpdateRequest.failed(payment.getOrderKey()));
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제 요청에 실패했습니다.");
        }
    }

    @Transactional
    public void handlePaymentCallback(PaymentV1Dto.PaymentCallbackRequest request) {
        Payment payment = paymentService.getPendingPaymentByOrderKey(request.orderId());

        String status = request.status();
        if ("SUCCESS".equals(status)) {
            eventPublisher.publishEvent(PaymentEvent.PaymentCompleted.from(payment));
        } else if ("FAILED".equals(status)) {
            eventPublisher.publishEvent(PaymentEvent.PaymentFailed.from(payment, request.reason()));
        } else {
            log.warn("알 수 없는 결제 상태 콜백 수신: orderId={}, status={}", request.orderId(), status);
        }
    }

    @Transactional
    public void syncPaymentStatus(Payment payment) {
        Order order = orderService.getOrderByOrderKey(payment.getOrderKey());
        User user = userService.getUserById(order.getUserId());
        Payment updatedPayment = paymentService.checkPaymentStatusFromPg(payment, user.getLoginId().getId());

        if (updatedPayment.isCompleted()) {
            eventPublisher.publishEvent(PaymentEvent.PaymentStatusUpdateRequest.completed(
                    payment.getOrderKey(),
                    updatedPayment.getTransactionKey()
            ));
        } else if (updatedPayment.isFailed()) {
            eventPublisher.publishEvent(PaymentEvent.PaymentStatusUpdateRequest.failed(
                    payment.getOrderKey()
            ));
        }
    }

}
