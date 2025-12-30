package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
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

    // Product 객체 대신 ID와 스냅샷 데이터 저장
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Builder
    private OrderItem(
            Integer quantity,
            Long productId,
            String productName,
            BigDecimal productPrice,
            Order order
    ) {
        // 필드 세팅
        this.quantity = quantity;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        if(quantity != null && productPrice != null) {
            this.totalAmount = this.productPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.totalAmount = null;
        }
        this.order = order;

        // 모든 필드 일관성 검증
        guard();
    }

    @Override
    protected void guard() {
        // quantity 검증: null이 아니어야 하며, 0보다 커야 함
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity가 비어있을 수 없습니다.");
        } else if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : quantity는 0보다 커야 합니다.");
        }
        
        // productId 검증: null이 아니어야 함 (주문 상품 ID 필수)
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : productId가 비어있을 수 없습니다.");
        }

        // productName 검증: null이 아니어야 함 (주문 시점 상품명 필수)
        if (productName == null || productName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : productName이 비어있을 수 없습니다.");
        }

        // productPrice 검증: null이 아니어야 하며, 0보다 커야 함
        if (productPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : productPrice가 비어있을 수 없습니다.");
        } else if (productPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : productPrice는 0보다 커야 합니다.");
        }

        // order 검증: null이 아니어야 함 (주문 정보 필수)
        if (order == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : order가 비어있을 수 없습니다.");
        }

        // totalAmount 검증: null이 아니어야 하며, 0보다 커야 함
        if (totalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount가 비어있을 수 없습니다.");
        } else if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "OrderItem : totalAmount는 0보다 커야 합니다.");
        }
    }
    
}

