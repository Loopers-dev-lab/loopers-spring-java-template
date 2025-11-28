package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.Money;
import com.loopers.domain.orderitem.OrderItem;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total_price", nullable = false))
    private Money totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(User user, Map<Product, Integer> productQuantities) {
        validateUser(user);
        validateProductQuantities(productQuantities);

        // 총 금액 계산
        Money calculatedTotal = calculateTotalPrice(productQuantities);

        // 포인트 부족 검증
        validateUserPoint(user, calculatedTotal);

        // 재고 부족 검증
        validateProductStock(productQuantities);

        this.user = user;
        this.status = OrderStatus.INIT;
        this.totalPrice = Money.zero();

        // OrderItem 생성 및 총 금액 계산
        productQuantities.forEach((product, quantity) -> {
            OrderItem orderItem = new OrderItem(this, product, quantity);
            this.orderItems.add(orderItem);
            this.totalPrice = this.totalPrice.add(orderItem.getTotalPrice());
        });
    }

    public static Order createOrder(User user, Map<Product, Integer> productQuantities) {
        return new Order(user, productQuantities);
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자는 필수입니다");
        }
    }

    private void validateProductQuantities(Map<Product, Integer> productQuantities) {
        if (productQuantities == null || productQuantities.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상품은 필수입니다");
        }
    }

    private void validateStatusUpdate(OrderStatus status) {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태는 필수입니다");
        }
    }

    private void validateUserPoint(User user, Money totalPrice) {
        if (user.getPoint().isLessThan(totalPrice)) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "포인트가 부족합니다. 현재 포인트: " + user.getPoint().getAmount() + ", 필요 포인트: " + totalPrice.getAmount());
        }
    }

    private void validateProductStock(Map<Product, Integer> productQuantities) {
        productQuantities.forEach((product, quantity) -> {
            if (!product.getStock().isSufficient(quantity)) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                        "재고가 부족합니다. 상품: " + product.getProductName() + ", 현재 재고: " + product.getStockQuantity());
            }
        });
    }

    public void updateStatus(OrderStatus status) {
        validateStatusUpdate(status);
        this.status = status;
    }

    private Money calculateTotalPrice(Map<Product, Integer> productQuantities) {
        return productQuantities.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(entry.getValue()))
                .reduce(Money.zero(), Money::add);
    }
}
