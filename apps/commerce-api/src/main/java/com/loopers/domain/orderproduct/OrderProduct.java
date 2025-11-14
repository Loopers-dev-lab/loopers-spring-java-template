package com.loopers.domain.orderproduct;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Entity
@Table(name = "order_product")
@Getter
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id = 0L;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price", nullable = false))
    private Money price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Product product;

    public OrderProduct(Order order, Product product, int quantity) {
        validateOrder(order);
        validateProduct(product);
        validateQuantity(quantity);

        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice();
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문은 필수입니다");
        }
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품은 필수입니다");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다");
        }
    }

    public Money getTotalPrice() {
        return this.price.multiply(this.quantity);
    }
}
