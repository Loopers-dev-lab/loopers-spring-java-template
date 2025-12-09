package com.loopers.domain.payment;

import com.loopers.application.payment.PgPayResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  @Transactional
  public Payment
  createPendingPayment(Payment payment) {
    return paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public Payment getPayment(String id) {
    return paymentRepository.findById(Long.valueOf(id))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with id: " + id));
  }

  @Transactional
  public Payment updateWithPgResult(Payment payment, PgPayResponse pgResponse) {
    payment.applyPgResult(pgResponse);
    return paymentRepository.save(payment);
  }

  @Transactional
  public void updatePaymentFromCallback(Payment payment) {
    paymentRepository.save(payment);
  }

  @Transactional(readOnly = true)
  public List<Payment> getPendingPayments() {
    return paymentRepository.findByStatus(PaymentStatus.PENDING);
  }

  @Transactional(readOnly = true)
  public List<Payment> getRecentFailedPayments(int hoursAgo) {
    return paymentRepository.findRecentFailedPayments(hoursAgo);
  }

  @Transactional(readOnly = true)
  public Payment getPaymentByOrderId(String orderId) {
    return paymentRepository.findByOrderId(Long.valueOf(orderId))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with orderId: " + orderId));
  }
}
