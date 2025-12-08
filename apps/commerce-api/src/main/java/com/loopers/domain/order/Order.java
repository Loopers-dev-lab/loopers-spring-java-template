package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.Money;
import com.loopers.domain.issuedcoupon.IssuedCoupon;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_coupon_id", referencedColumnName = "id")
    private IssuedCoupon issuedCoupon;

    private Order(User user, Map<Product, Integer> productQuantities, com.loopers.domain.coupon.Coupon coupon, com.loopers.domain.issuedcoupon.IssuedCoupon issuedCoupon) {
        validateUser(user);
        validateProductQuantities(productQuantities);

        // 재고 부족 검증
        validateProductStock(productQuantities);

        this.user = user;
        this.status = OrderStatus.INIT;
        this.totalPrice = Money.zero();
        this.issuedCoupon = issuedCoupon;

        // OrderItem 생성 및 총 금액 계산
        productQuantities.forEach((product, quantity) -> {
            OrderItem orderItem = new OrderItem(this, product, quantity);
            this.orderItems.add(orderItem);
            this.totalPrice = this.totalPrice.add(orderItem.getTotalPrice());
        });

        // 쿠폰 할인 적용 (최종 totalPrice에 반영)
        if (coupon != null) {
            this.totalPrice = coupon.getDiscount().applyDiscount(this.totalPrice);
        }
    }

    public static Order createOrder(User user, Map<Product, Integer> productQuantities, com.loopers.domain.coupon.Coupon coupon, com.loopers.domain.issuedcoupon.IssuedCoupon issuedCoupon) {
        return new Order(user, productQuantities, coupon, issuedCoupon);
    }

    public static Order createOrder(User user, Map<Product, Integer> productQuantities) {
        return new Order(user, productQuantities, null, null);
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

    public void completeOrder() {
        if (this.status != OrderStatus.INIT &&
            this.status != OrderStatus.RECEIVED &&
            this.status != OrderStatus.PAYMENT_COMPLETE) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문을 완료할 수 없는 상태입니다");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancelOrder() {
        if (this.status == OrderStatus.CANCELED) {
            return;  // 이미 취소됨, 멱등성 보장
        }
        if (this.status == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 완료된 주문은 취소할 수 없습니다");
        }
        this.status = OrderStatus.CANCELED;
    }

    private Money calculateTotalPrice(Map<Product, Integer> productQuantities) {
        return productQuantities.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(entry.getValue()))
                .reduce(Money.zero(), Money::add);
    }
}
