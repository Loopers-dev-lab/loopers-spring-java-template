package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "order_items")
@Entity
public class OrderItem extends BaseEntity {

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer productPrice;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long orderId;

    public static OrderItem create(final Integer quantity, final Integer productPrice, final Long productId,
                     final Long orderId) {
        validateOrderItem(quantity, productPrice, productId, orderId);
        OrderItem orderItem = new OrderItem();
        orderItem.quantity = quantity;
        orderItem.productPrice = productPrice;
        orderItem.productId = productId;
        orderItem.orderId = orderId;
        return orderItem;
    }

    private static void validateOrderItem(final Integer quantity, final Integer productPrice, final Long productId, final Long orderId) {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 개수는 비어있을 수 없습니다.");
        }
        if (productPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 가격은 비어있을 수 없습니다.");
        }
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 비어있을 수 없습니다.");
        }
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문은 비어있을 수 없습니다.");
        }
    }
}
