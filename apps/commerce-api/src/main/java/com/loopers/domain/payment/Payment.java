package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "payment",
    indexes = {
        @Index(name = "idx_payment_order_id", columnList = "ref_order_id"),
        @Index(name = "idx_payment_user_id", columnList = "ref_user_id"),
        @Index(name = "idx_payment_status_requested_at", columnList = "status, pg_requested_at"),
        @Index(name = "idx_payment_transaction_key", columnList = "transaction_key")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

  @Column(name = "ref_order_id", nullable = false)
  private Long orderId;

  @Column(name = "ref_user_id", nullable = false)
  private Long userId;

  @Column(name = "transaction_key", unique = true)
  private String transactionKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus status;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "amount", nullable = false))
  private Money amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type", nullable = false)
  private CardType cardType;

  @Column(name = "card_no", nullable = false)
  private String cardNo;

  @Column(name = "failure_reason")
  private String failureReason;

  @Column(name = "pg_requested_at", nullable = false)
  private LocalDateTime pgRequestedAt;

  @Column(name = "pg_completed_at")
  private LocalDateTime pgCompletedAt;

  private Payment(
      Long orderId,
      Long userId,
      CardType cardType,
      String cardNo,
      Money amount,
      LocalDateTime pgRequestedAt
  ) {
    validateOrderId(orderId);
    validateUserId(userId);
    validateCardType(cardType);
    validateCardNo(cardNo);
    validateAmount(amount);
    validatePgRequestedAt(pgRequestedAt);

    this.orderId = orderId;
    this.userId = userId;
    this.cardType = cardType;
    this.cardNo = cardNo;
    this.amount = amount;
    this.status = PaymentStatus.REQUESTED;
    this.pgRequestedAt = pgRequestedAt;
  }

  public static Payment of(
      Long orderId,
      Long userId,
      CardType cardType,
      String cardNo,
      Long amount,
      LocalDateTime pgRequestedAt
  ) {
    validateAmountValue(amount);
    return new Payment(orderId, userId, cardType, cardNo, Money.of(amount), pgRequestedAt);
  }

  public Long getAmountValue() {
    return amount.getValue();
  }


  public void toPending(String transactionKey) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    if (this.status != PaymentStatus.REQUESTED) {
      throw new CoreException(ErrorType.PAYMENT_CANNOT_MARK_PENDING);
    }
    this.transactionKey = transactionKey;
    this.status = PaymentStatus.PENDING;
  }

  public void toRequestFailed() {
    if (this.status != PaymentStatus.REQUESTED) {
      throw new CoreException(ErrorType.PAYMENT_CANNOT_MARK_REQUEST_FAILED);
    }
    this.status = PaymentStatus.REQUEST_FAILED;
  }


  public void toSuccess(LocalDateTime completedAt) {
    validatePending();
    this.status = PaymentStatus.SUCCESS;
    this.pgCompletedAt = completedAt;
  }


  public void toFailed(String reason, LocalDateTime completedAt) {
    validatePending();
    this.status = PaymentStatus.FAILED;
    this.failureReason = reason;
    this.pgCompletedAt = completedAt;
  }

  public boolean isCompleted() {
    return status.isCompleted();
  }

  private void validatePending() {
    if (this.status != PaymentStatus.PENDING) {
      throw new CoreException(ErrorType.PAYMENT_NOT_PENDING);
    }
  }

  private void validateOrderId(Long orderId) {
    if (orderId == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_ORDER_EMPTY);
    }
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_USER_EMPTY);
    }
  }

  private void validateCardType(CardType cardType) {
    if (cardType == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_CARD_TYPE_EMPTY);
    }
  }

  private void validateCardNo(String cardNo) {
    if (cardNo == null || cardNo.isBlank()) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_CARD_NO_EMPTY);
    }
  }

  private void validateAmount(Money amount) {
    if (amount == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_AMOUNT_EMPTY);
    }
  }

  private static void validateAmountValue(Long amount) {
    if (amount == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_AMOUNT_EMPTY);
    }
    if (amount <= 0) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_AMOUNT_NOT_POSITIVE);
    }
  }

  private void validatePgRequestedAt(LocalDateTime pgRequestedAt) {
    if (pgRequestedAt == null) {
      throw new CoreException(ErrorType.INVALID_PAYMENT_REQUESTED_AT_EMPTY);
    }
  }
}
