package com.loopers.domain.payment;

import com.loopers.application.payment.TransactionStatus;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Money;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "commerce_payments")
@Getter
public class Payment extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "card_type")
  private CardType cardType;

  @Column(name = "card_no")
  private String cardNo;

  @Embedded
  private Money amount;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Column(name = "transaction_key", unique = true)
  private String transactionKey;

  @Column(name = "reason")
  private String reason;

  protected Payment() {
  }

  private Payment(Long orderId, CardType cardType, String cardNo, Money amount, PaymentStatus status, String transactionKey, String reason) {
    this.orderId = orderId;
    this.cardType = cardType;
    this.cardNo = cardNo;
    this.amount = amount;
    this.status = status;
    this.transactionKey = transactionKey;
    this.reason = reason;
  }

  public static Payment create(Long orderId, CardType cardType, String cardNo, Money amount) {
    return new Payment(orderId, cardType, cardNo, amount, PaymentStatus.PENDING, null, null);
  }

  public void processInitialResponse(boolean isSuccess, String transactionKey) {
    if (isSuccess) {
      this.status = PaymentStatus.PENDING;
      this.transactionKey = transactionKey;
    } else {
      this.status = PaymentStatus.FAILED;
    }
  }

  public void approved() {
    this.status = PaymentStatus.APPROVED;
  }

  public void failed(String reason) {
    this.status = PaymentStatus.FAILED;
    this.reason = reason;
  }

  public void cancel() {
    this.status = PaymentStatus.CANCELLED;
  }

  public void processCallbackStatus(TransactionStatus callbackStatus, String reason) {
    switch (callbackStatus.name().toUpperCase()) {
      case "SUCCESS" -> approved();
      case "FAILED" -> failed(reason);
      case "CANCELLED" -> cancel();
      default -> throw new IllegalArgumentException("Unknown callback status: " + callbackStatus);
    }
  }
}
