package com.loopers.domain.payment;

import com.loopers.application.payment.TransactionStatus;
import com.loopers.application.event.PaymentSuccessEvent;
import com.loopers.application.event.PaymentFailureEvent;
import com.loopers.domain.order.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrderService orderService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public Payment requestPayment(String orderId, CardType cardType, String cardNo, Money amount) {
    Payment payment = Payment.create(orderId
        , cardType, cardNo
        , amount);
    return paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment getPayment(Long id) {
    return paymentRepository.findById(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with id: " + id));
  }

  @Transactional
  public Payment processPaymentRequest(Payment payment, boolean isSuccess, String transactionKey) {
    payment.processInitialResponse(isSuccess, transactionKey);
    return paymentRepository.save(payment);
  }

  @Transactional
  public void processPaymentCallback(String orderId, TransactionStatus status, String reason) {
    Payment payment = findPaymentByOrderId(orderId);
    payment.processCallbackStatus(status, reason);
    paymentRepository.save(payment);

    // 상태에 따른 이벤트 발행
    Order order = orderService.getOrderByOrderId(orderId);

    if (status == TransactionStatus.SUCCESS) {
      eventPublisher.publishEvent(new PaymentSuccessEvent(
          order.getOrderId(),
          order.getRefUserId(),
          payment.getAmount(),
          reason,
          order.getTotalPrice()
      ));
    } else if (status == TransactionStatus.FAILED) {
      eventPublisher.publishEvent(new PaymentFailureEvent(
          order.getOrderId(),
          order.getRefUserId(),
          payment.getAmount(),
          reason
      ));
    }
    // PENDING 상태는 별도 처리하지 않음
  }

  @Transactional(readOnly = true)
  public List<Payment> findPendingPayments() {
    return paymentRepository.findByStatus(PaymentStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<Payment> findRecentFailedPayments(int hoursAgo) {
    return paymentRepository.findRecentFailedPayments(hoursAgo);
  }

  @Transactional(readOnly = true)
  public Payment findPaymentByOrderId(String orderId) {
    return paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with orderId: " + orderId));
  }
}
