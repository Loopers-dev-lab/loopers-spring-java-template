package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "order_items")
@Getter
public class OrderItem extends BaseEntity {

    private Long productId;

    private String productName;

    private Integer price;

    private Integer quantity;

    @Builder
    private OrderItem(Long productId, String productName, Integer price, Integer quantity) {
        validate(productId, productName, price, quantity);
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public OrderItem() {
    }

    public static OrderItem createOrderItem(Long productId, String productName, Integer price, Integer quantity) {
        return OrderItem.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .quantity(quantity)
                .build();
    }

    private void validate(Long productId, String productName, Integer price, Integer quantity) {
        validateProductId(productId);
        validateProductName(productName);
        validatePrice(price);
        validateQuantity(quantity);
    }

    private void validateProductId(Long productId) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
    }

    private void validateProductName(String productName) {
        if (productName == null || productName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
    }

    private void validatePrice(Integer price) {
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 필수입니다.");
        }
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
        }
    }
}
