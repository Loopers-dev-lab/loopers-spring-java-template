package com.loopers.domain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 항목 Entity
 * 주문에 포함된 개별 상품 정보를 나타낸다
 */
public class OrderItem {
    private final Long id;
    private final Long productId;
    private final String productName;
    private final BigDecimal price;
    private final int quantity;
    private final BigDecimal subtotal;
    private final LocalDateTime createdAt;

    // 생성자
    private OrderItem(Long id, Long productId, String productName, 
                     BigDecimal price, int quantity, LocalDateTime createdAt) {
        validateProductId(productId);
        validateProductName(productName);
        validatePrice(price);
        validateQuantity(quantity);

        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.subtotal = calculateSubtotal(price, quantity);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * 신규 주문 항목 생성 (ID 없음)
     */
    public static OrderItem create(Long productId, String productName, BigDecimal price, int quantity) {
        return new OrderItem(null, productId, productName, price, quantity, null);
    }

    /**
     * 기존 주문 항목 재구성 (ID 있음)
     */
    public static OrderItem reconstruct(Long id, Long productId, String productName, 
                                       BigDecimal price, int quantity, LocalDateTime createdAt) {
        return new OrderItem(id, productId, productName, price, quantity, createdAt);
    }

    // === Business Logic ===

    /**
     * 소계 계산
     */
    private static BigDecimal calculateSubtotal(BigDecimal price, int quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * 특정 상품인지 확인
     */
    public boolean isProduct(Long productId) {
        return this.productId.equals(productId);
    }

    // === Validation Methods ===

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다");
        }
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("가격은 필수입니다");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다: " + price);
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + quantity);
        }
    }

    // === Getters ===

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
