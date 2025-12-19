package com.loopers.application.payment;

import com.loopers.application.event.payment.PaymentCompletedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentInfo requestPayment(String userId, PaymentCommand.RequestPayment command) {
        return paymentService.requestPayment(userId, command);
    }

    @Transactional
    public PaymentInfo processCallback(String userId, PaymentCommand.ProcessCallback command) {
        PaymentInfo paymentInfo = paymentService.processCallback(userId, command);

        // 기존 직접 업데이트 제거, 이벤트 발행으로 대체
        eventPublisher.publishEvent(new PaymentCompletedEvent(
            paymentInfo.orderNo(),
            paymentInfo.id(),
            paymentInfo.status(),
            paymentInfo.reason(),
            LocalDateTime.now()
        ));

        log.info("[PaymentFacade] 결제 콜백 처리 및 이벤트 발행 - orderNo: {}, status: {}",
                 paymentInfo.orderNo(), paymentInfo.status());

        return paymentInfo;
    }

    @Transactional
    public PaymentInfo syncPaymentStatus(String userId, String transactionKey) {
        PaymentInfo paymentInfo = paymentService.syncPaymentStatus(userId, transactionKey);

        // 이벤트 발행으로 대체
        eventPublisher.publishEvent(new PaymentCompletedEvent(
            paymentInfo.orderNo(),
            paymentInfo.id(),
            paymentInfo.status(),
            paymentInfo.reason(),
            LocalDateTime.now()
        ));

        log.info("[PaymentFacade] 결제 상태 동기화 및 이벤트 발행 - orderNo: {}, status: {}",
                 paymentInfo.orderNo(), paymentInfo.status());

        return paymentInfo;
    }

    @Transactional
    public PaymentInfo cancelPayment(String userId, String orderNo, String reason) {
        Payment payment = paymentService.findByOrderNo(orderNo);

        if (payment.getStatus() == com.loopers.domain.payment.PaymentStatus.SUCCESS) {
            throw new com.loopers.support.error.CoreException(
                    com.loopers.support.error.ErrorType.BAD_REQUEST,
                    "이미 성공한 결제는 취소할 수 없습니다.");
        }

        paymentService.cancelPayment(payment.getId(), reason);

        Order order = payment.getOrder();
        if (!order.isPaid()) {
            order.cancel();
            orderRepository.save(order);

            order.getItems().forEach(item ->
                    productRepository.increaseStock(item.getProductId(), item.getQuantity()));
        }

        log.info("[PaymentFacade] 결제 취소 완료 - orderNo: {}, reason: {}", orderNo, reason);
        return PaymentInfo.from(paymentService.findById(payment.getId()));
    }

    @Transactional(readOnly = true)
    public PaymentInfo getPaymentByOrderNo(String orderNo) {
        return paymentService.getPaymentByOrderNo(orderNo);
    }

    @Transactional(readOnly = true)
    public List<PaymentInfo> getPendingPaymentsOlderThan(LocalDateTime dateTime) {
        return paymentService.getPendingPaymentsOlderThan(dateTime);
    }

    @Transactional(readOnly = true)
    public List<PaymentInfo> getPaymentsRequiringRetry() {
        return paymentService.getPaymentsRequiringRetry();
    }
}
