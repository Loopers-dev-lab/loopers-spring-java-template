package com.loopers.domain.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "stocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", unique = true, nullable = false)
    private Long productId;

    private int quantity;

    public Stock(Long productId, int quantity) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품ID는 null이거나 0이하일 수 없습니다.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("재고는 0보다 작을 수 없습니다.");
        }
        this.productId = productId;
        this.quantity = quantity;
    }

    public static Stock create(Long productId, int quantity) {
        return new Stock(productId, quantity);
    }

    public void decrease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다.");
        }
        if (this.quantity < quantity) {
            throw new IllegalArgumentException("주문 상품의 재고가 부족합니다.");
        }
        this.quantity -= quantity;
    }
}
