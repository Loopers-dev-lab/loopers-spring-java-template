package com.loopers.domain.order;

import java.math.BigDecimal;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.dto.OrderItemDomainCreateRequest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_order_id", columnList = "order_id"),
        @Index(name = "idx_order_item_product_id", columnList = "product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "coupon_id", nullable = true)
    private Long couponId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    /**
     * 주문 항목 엔티티 생성자
     *
     * @param request 주문 항목 생성 요청 DTO
     * @throws IllegalArgumentException request가 null이거나 필수 값이 누락된 경우
     */
    private OrderItemEntity(OrderItemDomainCreateRequest request) {
        validateRequest(request);

        this.orderId = request.orderId();
        this.productId = request.productId();
        this.couponId = request.couponId();
        this.quantity = request.quantity();
        this.unitPrice = request.unitPrice();
        this.discountAmount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        this.totalPrice = calculateItemTotal();
    }

    /**
     * 주문 항목 생성 요청의 유효성을 검증합니다.
     *
     * @param request 주문 항목 생성 요청 DTO
     * @throws IllegalArgumentException 유효하지 않은 값이 포함된 경우
     */
    private void validateRequest(OrderItemDomainCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("주문 항목 생성 요청은 필수입니다.");
        }

        if (request.orderId() == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }

        if (request.productId() == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }

        if (request.quantity() == null) {
            throw new IllegalArgumentException("주문 수량은 필수입니다.");
        }

        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }

        if (request.unitPrice() == null) {
            throw new IllegalArgumentException("단가는 필수입니다.");
        }

        if (request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("단가는 0보다 커야 합니다.");
        }
    }

    /**
     * 주문 항목을 생성합니다.
     *
     * @param request 주문 항목 생성 요청 DTO
     * @return 생성된 주문 항목 엔티티
     */
    public static OrderItemEntity createOrderItem(OrderItemDomainCreateRequest request) {
        return new OrderItemEntity(request);
    }

    /**
     * 주문 항목의 총액을 계산합니다.
     *
     * <p>총액 = (단가 × 수량) - 할인 금액</p>
     *
     * @return 항목 총액 (할인 적용 후)
     */
    public BigDecimal calculateItemTotal() {
        BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return baseAmount.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }

    @Override
    protected void guard() {
        if (this.orderId == null) {
            throw new IllegalStateException("주문 ID는 필수입니다.");
        }

        if (this.productId == null) {
            throw new IllegalStateException("상품 ID는 필수입니다.");
        }

        if (this.quantity == null || this.quantity <= 0) {
            throw new IllegalStateException("주문 수량은 1 이상이어야 합니다.");
        }

        if (this.unitPrice == null || this.unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("단가는 0보다 커야 합니다.");
        }

        if (this.discountAmount == null || this.discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("할인 금액은 0 이상이어야 합니다.");
        }

        if (this.totalPrice == null || this.totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("총 가격은 0 이상이어야 합니다.");
        }

        // 총 가격 일치성 검증: (단가 × 수량) - 할인 금액 = 총 가격
        BigDecimal baseAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal calculatedTotal = baseAmount.subtract(discountAmount);
        if (this.totalPrice.compareTo(calculatedTotal) != 0) {
            throw new IllegalStateException(
                    String.format("총 가격 정합성 오류: (단가 × 수량)(%s) - 할인 금액(%s) ≠ 총 가격(%s)",
                            baseAmount, discountAmount, this.totalPrice)
            );
        }
    }
}
