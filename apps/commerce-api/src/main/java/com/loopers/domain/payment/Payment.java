package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.Order;
import com.loopers.interfaces.api.payment.PaymentMethod;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Embedded
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_no")
    private String cardNo;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    private Payment(Long orderId, Long userId, Money amount, PaymentMethod method) {
        validate(method);

        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = method;
    }

    public static Payment create(Order order, PaymentMethod paymentMethod) {
        return new Payment(
                order.getId(),
                order.getUserId(),
                order.calculateTotalPrice(),
                paymentMethod
        );
    }

    public void validate(PaymentMethod paymentMethod) {
        if (paymentMethod != PaymentMethod.CARD && paymentMethod != PaymentMethod.POINT) {
            throw new IllegalArgumentException("결제수단 정보가 없습니다.");
        }
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
