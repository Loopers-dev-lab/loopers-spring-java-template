package com.loopers.domain.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.dto.OrderDomainCreateRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user_id", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "original_total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal originalTotalAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "final_total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal finalTotalAmount;

    @Deprecated
    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrderStatus status;

    /**
     * 주문 엔티티 생성자
     *
     * @param request 주문 생성 요청 DTO
     * @throws IllegalArgumentException request가 null인 경우
     */
    private OrderEntity(OrderDomainCreateRequest request) {
        Objects.requireNonNull(request, "주문 생성 요청은 필수입니다.");
        Objects.requireNonNull(request.userId(), "사용자 ID는 필수입니다.");
        Objects.requireNonNull(request.originalTotalAmount(), "할인 전 총액은 필수입니다.");
        Objects.requireNonNull(request.discountAmount(), "할인 금액은 필수입니다.");
        Objects.requireNonNull(request.finalTotalAmount(), "할인 후 총액은 필수입니다.");

        if (request.originalTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 전 총액은 0보다 커야 합니다.");
        }

        if (request.discountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상이어야 합니다.");
        }

        if (request.finalTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 후 총액은 0보다 커야 합니다.");
        }

        // 금액 정합성 검증: 할인 전 총액 - 할인 금액 = 할인 후 총액
        BigDecimal calculatedFinalAmount = request.originalTotalAmount().subtract(request.discountAmount());
        if (calculatedFinalAmount.setScale(2, RoundingMode.HALF_UP)
                .compareTo(request.finalTotalAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException(
                    String.format("금액 정합성 오류: 할인 전 총액(%s) - 할인 금액(%s) = 할인 후 총액(%s)이 일치하지 않습니다. (계산 결과: %s)",
                            request.originalTotalAmount(), request.discountAmount(),
                            request.finalTotalAmount(), calculatedFinalAmount)
            );
        }

        this.userId = request.userId();
        this.originalTotalAmount = request.originalTotalAmount().setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = request.discountAmount().setScale(2, RoundingMode.HALF_UP);
        this.finalTotalAmount = request.finalTotalAmount().setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = this.finalTotalAmount; // 하위 호환성을 위해 유지
        this.status = OrderStatus.PENDING;
    }

    /**
     * 주문을 생성합니다.
     *
     * @param request 주문 생성 요청 DTO
     * @return 생성된 주문 엔티티
     */
    public static OrderEntity createOrder(OrderDomainCreateRequest request) {
        Objects.requireNonNull(request, "주문 생성 요청은 null일 수 없습니다.");
        return new OrderEntity(request);
    }

    /**
     * 주문을 확정합니다.
     * PENDING 상태에서만 CONFIRMED로 변경 가능합니다.
     */
    public void confirmOrder() {
        if (this.status != OrderStatus.PENDING || this.getDeletedAt() != null) {
            throw new CoreException(
                    ErrorType.INVALID_ORDER_STATUS,
                    String.format("주문 확정은 대기 상태 또는 활성화된 주문만 가능합니다. (현재 상태: %s)", this.status)
            );
        }

        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문이 대기 상태인지 확인합니다.
     *
     * @return 대기 상태 여부
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * 주문이 확정 상태인지 확인합니다.
     *
     * @return 확정 상태 여부
     */
    public boolean isConfirmed() {
        return this.status == OrderStatus.CONFIRMED;
    }

    /**
     * 주문을 취소합니다.
     *
     * <p>PENDING 또는 CONFIRMED 상태의 주문만 취소할 수 있습니다.</p>
     *
     * @throws IllegalStateException 취소할 수 없는 상태인 경우
     */
    public void cancelOrder() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    String.format("PENDING 또는 CONFIRMED 상태의 주문만 취소할 수 있습니다. 현재 상태: %s", this.status)
            );
        }
        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 주문이 취소 상태인지 확인합니다.
     *
     * @return 취소 상태 여부
     */
    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    @Override
    protected void guard() {
        if (this.userId == null) {
            throw new IllegalStateException("사용자 ID는 필수입니다.");
        }

        if (this.originalTotalAmount == null || this.originalTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("할인 전 총액은 0보다 커야 합니다.");
        }

        if (this.discountAmount == null || this.discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("할인 금액은 0 이상이어야 합니다.");
        }

        if (this.finalTotalAmount == null || this.finalTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("할인 후 총액은 0보다 커야 합니다.");
        }

        // 금액 정합성 검증
        BigDecimal calculatedFinalAmount = this.originalTotalAmount.subtract(this.discountAmount);
        if (calculatedFinalAmount.compareTo(this.finalTotalAmount) != 0) {
            throw new IllegalStateException(
                    String.format("금액 정합성 오류: 할인 전 총액(%s) - 할인 금액(%s) ≠ 할인 후 총액(%s)",
                            this.originalTotalAmount, this.discountAmount, this.finalTotalAmount)
            );
        }

        if (this.status == null) {
            throw new IllegalStateException("주문 상태는 필수입니다.");
        }

    }
}
