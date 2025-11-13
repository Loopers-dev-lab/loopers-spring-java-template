package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.orderproduct.OrderProduct;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @Column(name = "total_price", nullable = false, columnDefinition = "int default 0")
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProduct> orderProducts = new ArrayList<>();

    private Order(User user, Map<Product, Integer> productQuantities) {
        validateUser(user);
        validateProductQuantities(productQuantities);

        // 총 금액 계산
        BigDecimal calculatedTotal = calculateTotalPrice(productQuantities);

        // 포인트 부족 검증
        validateUserPoint(user, calculatedTotal);

        // 재고 부족 검증
        validateProductStock(productQuantities);

        this.user = user;
        this.status = OrderStatus.INIT;
        this.totalPrice = BigDecimal.ZERO;

        // OrderProduct 생성 및 총 금액 계산
        productQuantities.forEach((product, quantity) -> {
            OrderProduct orderProduct = new OrderProduct(this, product, quantity);
            this.orderProducts.add(orderProduct);
            this.totalPrice = this.totalPrice.add(orderProduct.getTotalPrice());
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

    private void validateUserPoint(User user, BigDecimal totalPrice) {
        if (user.getPoint().compareTo(totalPrice) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "포인트가 부족합니다. 현재 포인트: " + user.getPoint() + ", 필요 포인트: " + totalPrice);
        }
    }

    private void validateProductStock(Map<Product, Integer> productQuantities) {
        productQuantities.forEach((product, quantity) -> {
            if (product.getStock() < quantity) {
                throw new CoreException(ErrorType.BAD_REQUEST,
                        "재고가 부족합니다. 상품: " + product.getProductName() + ", 현재 재고: " + product.getStock());
            }
        });
    }

    public void updateStatus(OrderStatus status) {
        validateStatusUpdate(status);
        this.status = status;
    }

    private BigDecimal calculateTotalPrice(Map<Product, Integer> productQuantities) {
        return productQuantities.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
