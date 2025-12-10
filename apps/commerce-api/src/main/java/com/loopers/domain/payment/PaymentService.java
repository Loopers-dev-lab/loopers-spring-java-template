package com.loopers.domain.payment;

import com.loopers.application.payment.PgClient;
import com.loopers.application.payment.PgPayRequest;
import com.loopers.application.payment.PgPayResponse;
import com.loopers.application.payment.TransactionStatus;
import com.loopers.domain.order.Money;
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
  private final PgClient pgClient;

  @Transactional
  public Payment requestPayment(Long orderId, CardType cardType, String cardNo, Money amount) {
    // 1) 도메인 초기 상태 저장
    Payment payment = Payment.create(orderId
        , cardType, cardNo
        , amount);
    Payment saved = paymentRepository.save(payment);

    // 2) PG 요청 (외부 통신)
    PgPayRequest pgRequest = new PgPayRequest(
        payment.getOrderId().toString(),
        payment.getCardType().name(),
        payment.getCardNo(),
        payment.getAmount().getAmount()
    );

    PgPayResponse pgResponse = pgClient.requestPayment(pgRequest);
    return processPaymentRequest(saved, pgResponse);
  }

  @Transactional(readOnly = true)
  public Payment getPayment(String id) {
    return paymentRepository.findById(Long.valueOf(id))
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "Payment not found with id: " + id));
  }

  @Transactional
  public Payment processPaymentRequest(Payment payment, PgPayResponse pgResponse) {
    payment.processInitialResponse(pgResponse);
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
