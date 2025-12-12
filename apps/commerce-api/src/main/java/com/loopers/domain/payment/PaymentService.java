package com.loopers.domain.payment;

import com.loopers.application.payment.TransactionStatus;
import com.loopers.domain.order.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  @Transactional
  public Payment requestPayment(Long orderId, CardType cardType, String cardNo, Money amount) {
    Payment payment = Payment.create(orderId
        , cardType, cardNo
        , amount);
    return paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment getPayment(String id) {
    return paymentRepository.findById(Long.valueOf(id))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with id: " + id));
  }

  @Transactional
  public Payment processPaymentRequest(Payment payment, boolean isSuccess, String transactionKey) {
    payment.processInitialResponse(isSuccess, transactionKey);
    return paymentRepository.save(payment);
  }

  @Transactional
  public void processPaymentCallback(Long orderId, TransactionStatus status, String reason) {
    Payment payment = findPaymentByOrderId(orderId);
    payment.processCallbackStatus(status, reason);
    paymentRepository.save(payment);
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
  public Payment findPaymentByOrderId(Long orderId) {
    return paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with orderId: " + orderId));
  }
}
