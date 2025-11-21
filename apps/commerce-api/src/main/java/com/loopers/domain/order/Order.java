package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order extends BaseEntity {

    private BigDecimal finalAmount = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            User user
    ) {
        this.discountAmount = discountAmount;
        this.shippingFee = shippingFee;
        this.user = user;

        // 유효성 검사
        guard();
    }

    /**
     * Order 엔티티의 유효성 검사
     */
    @Override
    protected void guard() {

        // discountAmount 검증: null이 아니어야 하며, 0 이상이어야 함
        if (discountAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount가 비어있을 수 없습니다.");
        } else if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount는 음수가 될 수 없습니다.");
        }

        // shippingFee 검증: null이 아니어야 하며, 0 이상이어야 함
        if (shippingFee == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : shippingFee가 비어있을 수 없습니다.");
        } else if (shippingFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : shippingFee는 음수가 될 수 없습니다.");
        }

        // orderStatus 검증: null이 아니어야 함
        if (orderStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : orderStatus가 비어있을 수 없습니다.");
        }

        // user 검증: null이 아니어야 함 (주문한 사용자 정보 필수)
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : user가 비어있을 수 없습니다.");
        }
    }

    /**
     * 주문 상품 추가
     * 주문 상품을 리스트에 추가하고, 양방향 관계를 설정함
     */
    public void addOrderItem(Product product, Integer quantity) {
        
        // 주문 상태 검증
        if(this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : PENDING 상태의 주문만 주문 상품을 추가할 수 있습니다.");
        }

        // OrderItem 생성
        OrderItem orderItem = OrderItem.builder()
            .product(product)
            .quantity(quantity)
            .order(this)
            .build();

        // 주문 상품 추가
        this.orderItems.add(orderItem);

        // 총가격과 결제 금액 계산하기
        this.totalPrice = this.totalPrice.add(orderItem.getTotalAmount());
        this.finalAmount = this.totalPrice.subtract(this.discountAmount)
                                          .add(this.shippingFee);
    }

    /**
     * 할인 금액 적용 (쿠폰)
     * @param discountAmount 할인 금액
     */
    public void applyDiscount(BigDecimal discountAmount) {

        // 주문 상태 검증
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : PENDING 상태의 주문만 할인을 적용할 수 있습니다.");
        }

        // 할인 금액 검증
        if (discountAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount가 비어있을 수 없습니다.");
        } else if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : discountAmount는 음수가 될 수 없습니다.");
        }

        // 할인 금액 적용
        this.discountAmount = this.discountAmount.add(discountAmount);
        this.finalAmount = this.totalPrice.subtract(this.discountAmount)
                                          .add(this.shippingFee);
    }

    /**
     * 주문 확인 처리
     * PENDING 상태의 주문만 CONFIRMED 상태로 변경 가능
     */
    public void confirm() {
        if (this.orderStatus != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order : PENDING 상태의 주문만 확인할 수 있습니다.");
        }
        this.orderStatus = OrderStatus.CONFIRMED;
    }

}

