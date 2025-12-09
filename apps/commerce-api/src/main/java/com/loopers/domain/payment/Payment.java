package com.loopers.domain.payment;

import com.loopers.application.payment.PgPayResponse;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Money;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "commerce_payments")
@Getter
public class Payment extends BaseEntity {

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "card_type")
  private CardType cardType;

  @Column(name = "card_no")
  private String cardNo;

  @Embedded
  private Money amount;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @Column(name = "transaction_key")
  private String transactionKey;

  protected Payment() {
  }

  private Payment(Long orderId, CardType cardType, String cardNo, Money amount, PaymentStatus status, String transactionKey) {
    this.orderId = orderId;
    this.cardType = cardType;
    this.cardNo = cardNo;
    this.amount = amount;
    this.status = status;
    this.transactionKey = transactionKey;
  }

  public static Payment create(Long orderId, CardType cardType, String cardNo, Money amount) {
    return new Payment(orderId, cardType, cardNo, amount, PaymentStatus.PENDING, null);
  }

  public void applyPgResult(PgPayResponse response) {
    if (response.isSuccess()) {
      this.status = PaymentStatus.PENDING;
      this.transactionKey = response.transactionKey();
    } else {
      this.status = PaymentStatus.FAILED;
    }
  }

  public void approved() {
    this.status = PaymentStatus.APPROVED;
  }
}
