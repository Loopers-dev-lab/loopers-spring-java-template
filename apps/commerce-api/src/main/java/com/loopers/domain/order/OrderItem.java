package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_price"))
    private Money unitPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_price"))
    private Money totalPrice;

    private OrderItem(Long product_id, int quantity, Money unitPrice, Money totalPrice) {

        if (product_id == null || product_id <= 0) {
            throw new IllegalArgumentException("상품 ID는 음수 혹은 null일 수 없습니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }

        validateTotalPrice(quantity, unitPrice, totalPrice);
        this.productId = product_id;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }

    // 토탈 금액 검증
    public void validateTotalPrice(int quantity, Money unitPrice, Money totalPrice) {
            if (!totalPrice.equals(unitPrice.multiply(quantity))) {
                throw new IllegalArgumentException("(수량 * 단가)가 총 주문 금액과 맞지 않습니다.");
            }
    }

    public static OrderItem create(Long productId, int quantity, Money unitPrice, Money totalPrice) {
        return new OrderItem(productId, quantity, unitPrice, totalPrice);
    }
}
