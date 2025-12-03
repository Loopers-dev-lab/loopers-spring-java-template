package com.loopers.domain.order;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 도메인 모델 (Aggregate Root)
 * - 순수 도메인 객체 (JPA 의존성 없음)
 * - 주문 항목들을 포함하는 집합 루트
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    private Long id;
    private String userId;
    private BigDecimal totalAmount;      // 상품 총액
    private int usedPoints;              // 사용한 포인트
    private BigDecimal finalAmount;      // 최종 결제 금액 (totalAmount - usedPoints)
    private OrderStatus status;
    private LocalDateTime orderedAt;
    private LocalDateTime modifiedAt;
    private List<OrderItem> orderItems;

    /**
     * 주문 생성 팩토리 메서드
     */
    public static Order create(String userId, List<OrderItem> orderItems, int usedPoints) {
        validateUserId(userId);
        validateOrderItems(orderItems);
        validateUsedPoints(usedPoints);

        BigDecimal totalAmount = calculateTotalAmount(orderItems);
        validatePointsNotExceedTotal(usedPoints, totalAmount);

        BigDecimal finalAmount = totalAmount.subtract(BigDecimal.valueOf(usedPoints));

        LocalDateTime now = LocalDateTime.now();

        return new Order(
                null,
                userId,
                totalAmount,
                usedPoints,
                finalAmount,
                OrderStatus.PENDING,
                now,
                now,
                new ArrayList<>(orderItems)  // 방어적 복사
        );
    }

    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Order reconstruct(
            Long id,
            String userId,
            List<OrderItem> orderItems,
            BigDecimal totalAmount,
            int usedPoints,
            BigDecimal finalAmount,
            OrderStatus status,
            LocalDateTime orderedAt,
            LocalDateTime modifiedAt
    ) {
        return new Order(
                id,
                userId,
                totalAmount,
                usedPoints,
                finalAmount,
                status,
                orderedAt,
                modifiedAt,
                new ArrayList<>(orderItems)
        );
    }

    /**
     * 주문 완료 처리
     */
    public void complete() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("취소된 주문은 완료할 수 없습니다");
        }
        if (this.status == OrderStatus.COMPLETED) {
            return; // 이미 완료됨 (멱등성)
        }

        this.status = OrderStatus.COMPLETED;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소
     */
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("완료된 주문은 취소할 수 없습니다");
        }
        if (this.status == OrderStatus.CANCELLED) {
            return; // 이미 취소됨 (멱등성)
        }

        this.status = OrderStatus.CANCELLED;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 주문 완료 여부
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.COMPLETED;
    }

    /**
     * 주문 취소 여부
     */
    public boolean isCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    /**
     * 주문 대기 여부
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * 주문 소유자 확인
     */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    /**
     * 총 금액 계산
     */
    private static BigDecimal calculateTotalAmount(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === Validation Methods ===

    private static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
    }

    private static void validateOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다");
        }
    }

    private static void validateUsedPoints(int usedPoints) {
        if (usedPoints < 0) {
            throw new IllegalArgumentException("사용 포인트는 0 이상이어야 합니다: " + usedPoints);
        }
    }

    private static void validatePointsNotExceedTotal(int usedPoints, BigDecimal totalAmount) {
        BigDecimal pointsAmount = BigDecimal.valueOf(usedPoints);
        if (pointsAmount.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException(
                    String.format("사용 포인트(%d)가 총 주문 금액(%s)을 초과할 수 없습니다",
                            usedPoints, totalAmount)
            );
        }
    }
}
