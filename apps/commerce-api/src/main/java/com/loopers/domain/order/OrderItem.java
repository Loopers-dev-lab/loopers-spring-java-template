package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Product 엔티티 직접 참조 제거 - 스냅샷 패턴 적용
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 100)
    private String brandName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal price;

    @Builder
    private OrderItem(Order order, Long productId, String productName, String brandName,
        Integer quantity, BigDecimal price) {
        validateProductId(productId);
        validateProductName(productName);
        validateQuantity(quantity);
        validatePrice(price);

        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.brandName = brandName;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Product 엔티티로부터 주문 항목 생성 (스냅샷 저장)
     */
    public static OrderItem from(Product product, Integer quantity) {
        return OrderItem.builder()
            .productId(product.getId())
            .productName(product.getName())
            .brandName(product.getBrand().getName())
            .quantity(quantity)
            .price(product.getPrice())
            .build();
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

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0원 이상이어야 합니다.");
        }
    }

    public BigDecimal calculateAmount() {
        return this.price.multiply(BigDecimal.valueOf(this.quantity));
    }

    void setOrder(Order order) {
        this.order = order;
    }
}
