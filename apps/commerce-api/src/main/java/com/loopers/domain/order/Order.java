package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 주문 도메인 엔티티.
 * <p>
 * 주문의 상태, 총액, 주문 아이템을 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "`order`")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Order extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(name = "discount_amount")
    private Integer discountAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "json")
    private List<OrderItem> items;

    /**
     * Create a new Order for the given user with the provided items, optional coupon, and optional discount.
     *
     * The constructor makes a defensive, immutable copy of the items and computes the order's total as
     * max(0, subtotal - discountAmount), where subtotal is the sum of item price × quantity and
     * discountAmount is treated as 0 when null.
     *
     * @param userId        the identifier of the user placing the order; must not be null
     * @param items         the list of order items; must not be null or empty
     * @param couponCode    optional coupon code to associate with the order
     * @param discountAmount optional discount amount to subtract from the subtotal; treated as 0 if null
     * @throws CoreException if userId is null or items is null or empty
     */
    public Order(Long userId, List<OrderItem> items, String couponCode, Integer discountAmount) {
        validateUserId(userId);
        validateItems(items);
        this.userId = userId;
        // ✅ 방어적 복사로 불변 리스트 생성 (총액과 아이템의 일관성 보장)
        List<OrderItem> immutableItems = List.copyOf(items);
        this.items = immutableItems;
        Integer subtotal = calculateTotalAmount(immutableItems);
        this.discountAmount = discountAmount != null ? discountAmount : 0;
        this.totalAmount = Math.max(0, subtotal - this.discountAmount);
        this.couponCode = couponCode;
        this.status = OrderStatus.PENDING;
    }

    /**
     * Create an Order for the specified user with the provided items.
     *
     * Creates the order without a coupon and without a discount.
     *
     * @param userId the identifier of the user placing the order; must not be null
     * @param items  the list of order items; must not be null or empty
     * @return       the created Order instance
     */
    public static Order of(Long userId, List<OrderItem> items) {
        return new Order(userId, items, null, null);
    }

    /**
     * Create a new Order using the provided user, items, optional coupon code, and discount amount.
     *
     * @param userId the ID of the user placing the order
     * @param items the list of order items; must not be null or empty
     * @param couponCode optional coupon code to associate with the order, or null if none
     * @param discountAmount discount amount to subtract from the subtotal, or null to treat as 0
     * @return the created Order
     */
    public static Order of(Long userId, List<OrderItem> items, String couponCode, Integer discountAmount) {
        return new Order(userId, items, couponCode, discountAmount);
    }

    /**
     * 주문 아이템 목록으로부터 총액을 계산합니다.
     *
     * @param items 주문 아이템 목록
     * @return 계산된 총액
     */
    private static Integer calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
            .mapToInt(item -> item.getPrice() * item.getQuantity())
            .sum();
    }

    /**
     * 사용자 ID의 유효성을 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws CoreException userId가 null일 경우
     */
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
    }

    /**
     * 주문 아이템 목록의 유효성을 검증합니다.
     *
     * @param items 검증할 주문 아이템 목록
     * @throws CoreException items가 null이거나 비어있을 경우
     */
    private void validateItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 아이템은 필수이며 최소 1개 이상이어야 합니다.");
        }
    }

    /**
     * 주문을 완료 상태로 변경합니다.
     * 상태 변경만 수행하며, 포인트 차감은 도메인 서비스에서 처리합니다.
     */
    public void complete() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("완료할 수 없는 주문 상태입니다. (현재 상태: %s)", this.status));
        }
        this.status = OrderStatus.COMPLETED;
    }

    /**
     * Mark the order as canceled.
     *
     * Only the order status is changed; any point refunds are handled by a domain service.
     * Only orders currently in PENDING or COMPLETED may be canceled.
     *
     * @throws CoreException if the order is not in PENDING or COMPLETED (error type BAD_REQUEST)
     */
    public void cancel() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.COMPLETED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("취소할 수 없는 주문 상태입니다. (현재 상태: %s)", this.status));
        }
        this.status = OrderStatus.CANCELED;
    }
}
