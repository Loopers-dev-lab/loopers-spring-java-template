package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  public Payment create(
      Long orderId,
      Long userId,
      CardType cardType,
      String cardNo,
      Long amount,
      LocalDateTime pgRequestedAt
  ) {
    Payment payment = Payment.of(orderId, userId, cardType, cardNo, amount, pgRequestedAt);
    return paymentRepository.save(payment);
  }

  public Payment getByTransactionKey(String transactionKey) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    return paymentRepository.findByTransactionKey(transactionKey)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
  }

  public List<Payment> findPendingPaymentsBefore(LocalDateTime before) {
    Objects.requireNonNull(before, "before는 null일 수 없습니다.");
    return paymentRepository.findByStatusAndPgRequestedAtBefore(PaymentStatus.PENDING, before);
  }

  public void toPending(Long paymentId, String transactionKey) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    payment.toPending(transactionKey);
    paymentRepository.save(payment);
  }

  public void toRequestFailed(Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
    payment.toRequestFailed();
    paymentRepository.save(payment);
  }

  public void toSuccess(Payment payment, LocalDateTime completedAt) {
    Objects.requireNonNull(payment, "payment는 null일 수 없습니다.");
    Objects.requireNonNull(completedAt, "completedAt은 null일 수 없습니다.");
    payment.toSuccess(completedAt);
    paymentRepository.save(payment);
  }

  public void toFailed(Payment payment, String reason, LocalDateTime completedAt) {
    Objects.requireNonNull(payment, "payment는 null일 수 없습니다.");
    Objects.requireNonNull(completedAt, "completedAt은 null일 수 없습니다.");
    payment.toFailed(reason, completedAt);
    paymentRepository.save(payment);
  }
}
