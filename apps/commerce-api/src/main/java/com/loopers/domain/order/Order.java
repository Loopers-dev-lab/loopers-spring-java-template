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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "json")
    private List<OrderItem> items;

    /**
     * Order 인스턴스를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param items 주문 아이템 목록
     * @throws CoreException items가 null이거나 비어있을 경우
     */
    public Order(Long userId, List<OrderItem> items) {
        validateUserId(userId);
        validateItems(items);
        this.userId = userId;
        this.items = items;
        this.totalAmount = calculateTotalAmount(items);
        this.status = OrderStatus.PENDING;
    }

    /**
     * Order 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param userId 사용자 ID
     * @param items 주문 아이템 목록
     * @return 생성된 Order 인스턴스
     */
    public static Order of(Long userId, List<OrderItem> items) {
        return new Order(userId, items);
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
     * 주문을 취소 상태로 변경합니다.
     * 상태 변경만 수행하며, 포인트 환불은 도메인 서비스에서 처리합니다.
     * PENDING 상태의 주문만 취소할 수 있습니다.
     */
    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("취소할 수 없는 주문 상태입니다. (현재 상태: %s)", this.status));
        }
        this.status = OrderStatus.CANCELED;
    }
}

