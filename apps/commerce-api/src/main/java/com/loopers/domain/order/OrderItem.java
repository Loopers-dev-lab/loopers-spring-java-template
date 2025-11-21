package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderItem extends BaseEntity {

    private Integer quantity;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder
    private OrderItem(
            Integer quantity,
            Product product,
            Order order
    ) {
        this.quantity = quantity;
        this.product = product;
        this.order = order;
        // 유효성 검사
        guard();

        // 계산하기
        this.unitPrice = product.getPrice();
        this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    protected void guard() {
        // quantity 검증: null이 아니어야 하며, 0보다 커야 함
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity가 비어있을 수 없습니다.");
        } else if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity는 0보다 커야 합니다.");
        }
        
        // product 검증: null이 아니어야 함 (주문 상품 정보 필수)
        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : product가 비어있을 수 없습니다.");
        }

        // order 검증: null이 아니어야 함 (주문 정보 필수)
        if (order == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : order가 비어있을 수 없습니다.");
        }
        
        // unitPrice 검증: null이고 0보다 커야 함
        if (unitPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : unitPrice가 비어있을 수 없습니다.");
        } else if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : unitPrice는 0보다 커야 합니다.");
        }

        // totalAmount 검증: null이고 0보다 커야 함
        if (totalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount가 비어있을 수 없습니다.");
        } else if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount는 0보다 커야 합니다.");
        }
    }

    // Order를 통해서만 OrderItem을 생성할 수 있도록 하는 메서드
    // order는 null일 수 있음 (나중에 setOrder로 설정 가능)
    // public static OrderItem create(Order order, Integer quantity, Product product) {
    //     OrderItem orderItem = new OrderItem(quantity, product);
    //     orderItem.order = order;
    //     return orderItem;
    // }

    // public void setOrder(Order order) {
    //     if (this.order != null && !this.order.equals(order)) {
    //         throw new CoreException(ErrorType.CONFLICT, "OrderItem : Order가 이미 존재합니다.");
    //     }
    //     this.order = order;
    // }

    
}

