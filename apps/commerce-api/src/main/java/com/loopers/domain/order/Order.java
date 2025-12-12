package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "orders")
@Getter
@Builder
public class Order extends BaseEntity {

    private Long userId;

    @Column(nullable = false, unique = true)
    private String orderKey;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @Builder.Default
    List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false)
    private Integer originalTotalPrice;

    private Long couponId;

    private Integer discountPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "receiverName", column = @Column(name = "receiver_name", nullable = false)),
            @AttributeOverride(name = "receiverPhoneNumber", column = @Column(name = "phone_number", nullable = false)),
            @AttributeOverride(name = "baseAddress", column = @Column(name = "base_address", nullable = false)),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "detail_address", nullable = false))
    })
    private Delivery delivery;

    public Order() {
    }

    private Order(Long userId, String orderKey, List<OrderItem> orderItems, Integer originalTotalPrice,
                  Long couponId, Integer discountPrice, OrderStatus orderStatus, Delivery delivery) {
        this.userId = userId;
        this.orderKey = orderKey;
        this.orderItems = orderItems;
        this.originalTotalPrice = originalTotalPrice;
        this.couponId = couponId;
        this.discountPrice = discountPrice;
        this.orderStatus = orderStatus;
        this.delivery = delivery;
    }

    public static Order createOrder(Long userId, String orderKey, Delivery delivery) {
        validateCreateOrder(userId, orderKey, delivery);
        return Order.builder()
                .userId(userId)
                .orderKey(orderKey)
                .originalTotalPrice(0)
                .couponId(null)
                .discountPrice(0)
                .orderStatus(OrderStatus.CREATED)
                .delivery(delivery)
                .build();
    }

    public void applyCoupon(Long couponId, Integer discountPrice) {
        if (couponId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 필수입니다.");
        }
        if (discountPrice == null || discountPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }
        this.couponId = couponId;
        this.discountPrice = discountPrice;
    }

    public void addOrderItem(OrderItem orderItem) {
        validateOrderItem(orderItem);
        orderItems.add(orderItem);
    }

    public int addPrice(int price) {
        validatePrice(price);
        if (this.originalTotalPrice == null) {
            this.originalTotalPrice = 0;
        }
        this.originalTotalPrice = this.originalTotalPrice + price;
        return this.originalTotalPrice;
    }

    public void pay() {
        if (this.orderStatus != OrderStatus.CREATED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 생성 상태인 경우에만 결제 완료 처리할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.orderStatus == OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 완료 상태인 경우 취소할 수 없습니다.");
        }
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void complete() {
        if (this.orderStatus != OrderStatus.PAID) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 완료 상태인 경우에만 주문 완료 처리할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.COMPLETED;
    }

    private static void validateCreateOrder(Long userId, String orderKey, Delivery delivery) {
        validateUserId(userId);
        validateOrderKey(orderKey);
        validateDelivery(delivery);
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    private static void validateOrderKey(String orderKey) {
        if (orderKey == null || orderKey.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 키는 필수입니다.");
        }
        if (orderKey.length() < 6) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 키는 최소 6자리 이상이어야 합니다.");
        }
    }

    private static void validateDelivery(Delivery delivery) {
        if (delivery == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "배송 정보는 필수입니다.");
        }
    }

    private void validateOrderItem(OrderItem orderItem) {
        if (orderItem == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상품은 필수입니다.");
        }
    }

    private void validatePrice(int price) {
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.");
        }
    }

    public boolean hasCoupon() {
        return this.couponId != null;
    }

    public void applyOriginalTotalPrice(int originalTotalPrice) {
        this.originalTotalPrice = originalTotalPrice;
    }

    public void applyDiscountPrice(int discountPrice) {
        this.discountPrice = discountPrice;
    }
}
