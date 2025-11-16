package com.loopers.domain.order;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 주문 아이템 Value Object.
 * <p>
 * 주문에 포함된 상품 정보와 수량을 나타냅니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Getter
@EqualsAndHashCode
@Embeddable
public class OrderItem {
    private Long productId;
    private String name;
    private Integer price;
    private Integer quantity;

    protected OrderItem() {
    }

    /**
     * OrderItem 인스턴스를 생성합니다.
     *
     * @param productId 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param quantity 수량
     */
    public OrderItem(Long productId, String name, Integer price, Integer quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * OrderItem 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param productId 상품 ID
     * @param name 상품 이름
     * @param price 상품 가격
     * @param quantity 수량
     * @return 생성된 OrderItem 인스턴스
     */
    public static OrderItem of(Long productId, String name, Integer price, Integer quantity) {
        return new OrderItem(productId, name, price, quantity);
    }
}

