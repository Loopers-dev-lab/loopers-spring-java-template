package com.loopers.domain.orderitem;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {
    @Column(name = "ref_order_id", nullable = false)
    private Long orderId;

    @Column(name = "ref_product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "order_price", nullable = false)
    private BigDecimal orderPrice;

    public OrderItem(Long orderId, Long productId, int quantity, BigDecimal orderPrice) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.");
        }

        if (orderPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 가격은 음수일 수 없습니다.");
        }

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderPrice = orderPrice;
    }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.");
        }
        this.quantity = quantity;
    }

    public void assignOrderId(Long orderId) {
        this.orderId = orderId;
    }



}
