package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.common.vo.Price;
import com.loopers.infrastructure.pg.PgV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_order",
        indexes = {
                @Index(name = "idx_order_order_id", columnList = "orderId"),
                @Index(name = "idx_order_user_id", columnList = "userId")
        }
)
@Getter
public class Order extends BaseEntity {
    // 외부에서 식별 가능한 uuid 형식의 orderId
    private String orderId;
    private Long userId;
    @ElementCollection
    @CollectionTable(
            name = "tb_order_item",
            joinColumns = @JoinColumn(name = "order_id")
    )
    private List<OrderItem> orderItems;
    @Convert(converter = Price.Converter.class)
    private Price originalPrice;
    private Long couponId;
    @Convert(converter = Price.Converter.class)
    private Price discountAmount;
    @Convert(converter = Price.Converter.class)
    private Price finalPrice;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // 결제 수단 (PG, POINT)
    private String pgTransactionKey; // 외부 결제 시스템에서 발급한 결제 ID
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String pgPaymentReason;
    private String cardType;  // SAMSUNG, etc.
    private String cardNo;  // 마스킹된 카드 번호
    private String callbackUrl; // 결제 완료 후 호출할 콜백 URL
    private Integer retryCount; // 재결제 시도 횟수

    protected Order() {
    }

    private Order(Long userId, List<OrderItem> orderItems, Price originalPrice, Long couponId, Price discountAmount, Price finalPrice, PaymentMethod paymentMethod) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.");
        }
        if (userId == null || userId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 1 이상이어야 합니다.");
        }
        if (couponId != null && discountAmount.amount() > originalPrice.amount()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액이 원가를 초과할 수 없습니다.");
        }
        if (paymentMethod == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단은 필수입니다.");
        }
        this.orderId = UUID.randomUUID().toString().replace("-", "");
        this.userId = userId;
        this.orderItems = orderItems;
        this.originalPrice = originalPrice;
        this.couponId = couponId;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
        this.paymentMethod = paymentMethod;
        this.retryCount = 0;
    }

    public static Order create(Long userId, List<OrderItem> orderItems, DiscountResult discountResult, PaymentMethod paymentMethod) {
        if (discountResult == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 결과는 null일 수 없습니다.");
        }
        return new Order(
                userId,
                orderItems,
                discountResult.originalPrice(),
                discountResult.couponId(),
                discountResult.discountAmount(),
                discountResult.finalPrice(),
                paymentMethod
        );
    }

    public static Order create(Long userId, List<OrderItem> orderItems, PaymentMethod paymentMethod) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.");
        }
        Price originalPrice = new Price(orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum());
        return create(userId, orderItems, new DiscountResult(originalPrice), paymentMethod);
    }

    public void setPaymentInfo(String cardType, String cardNo, String callbackUrl) {
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.callbackUrl = callbackUrl;
        this.status = OrderStatus.PENDING;
    }

    public void markPaidByPoint() {
        if (this.paymentMethod != PaymentMethod.POINT) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 결제 주문이 아닙니다.");
        }
        this.status = OrderStatus.PAID;
        this.pgTransactionKey = null;
        this.pgPaymentReason = null;
    }

    public void updateOrderStatus(PgV1Dto.PgTransactionResponse transactionResponse) {
        updatePaymentStatus(
                mapStatus(transactionResponse.status()),
                transactionResponse.transactionKey(),
                transactionResponse.reason()
        );
    }

    public void updatePaymentStatus(OrderStatus newStatus, String transactionKey, String reason) {
        if (newStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 상태가 없습니다.");
        }
        if (this.status == OrderStatus.PAID && newStatus != OrderStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 결제된 주문은 취소 상태로만 변경할 수 있습니다.");
        }
        this.status = newStatus;
        this.pgTransactionKey = transactionKey;
        this.pgPaymentReason = reason;
    }

    /**
     * 재결제 시도 횟수 증가
     */
    public void incrementRetryCount() {
        this.retryCount = this.retryCount + 1;
    }

    /**
     * 재결제 시도 가능 여부 확인
     * 최대 2회까지만 재시도 가능
     */
    public boolean canRetryPayment() {
        return this.retryCount < 2;
    }

    /**
     * 타임아웃으로 인한 실패 처리
     */
    public void markAsFailedByTimeout(String reason) {
        if (this.status == OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 결제 완료된 주문은 실패 처리할 수 없습니다.");
        }
        this.status = OrderStatus.FAILED;
        this.pgPaymentReason = reason;
    }

    private OrderStatus mapStatus(String status) {
        if (status == null) {
            return OrderStatus.PENDING;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> OrderStatus.PAID;
            case "FAILED" -> OrderStatus.FAILED;
            case "CANCELLED" -> OrderStatus.CANCELLED;
            default -> OrderStatus.PENDING;
        };
    }
}
