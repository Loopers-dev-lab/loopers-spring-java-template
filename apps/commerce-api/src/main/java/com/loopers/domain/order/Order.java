package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private ZonedDateTime canceledAt;

    @Builder
    private Order(String userId, OrderStatus status, BigDecimal totalAmount) {
        validateUserId(userId);
        this.userId = userId;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
            .map(OrderItem::calculateAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 쿠폰 할인을 적용하여 최종 결제 금액을 계산합니다.
     *
     * @param discountAmount 할인 금액
     * @return 최종 결제 금액 (0원 미만이면 0원)
     */
    public BigDecimal applyDiscount(BigDecimal discountAmount) {
        BigDecimal finalAmount = this.totalAmount.subtract(discountAmount);
        return finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount;
    }

    public boolean canCancel() {
        return this.status == OrderStatus.PENDING;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "배송 시작 후에는 취소할 수 없습니다.");
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = ZonedDateTime.now();
    }

    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "대기 중인 주문만 완료할 수 있습니다.");
        }
        this.status = OrderStatus.COMPLETED;
    }
}
