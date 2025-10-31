package com.loopers.domain.point;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "points")
public class Point {

    @Id
    private String userId;

    private Long amount;

    public static Point create(String userId, Long initialAmount) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("사용자 ID는 비어있을 수 없습니다.");
        }
        if (initialAmount == null || initialAmount < 0) {
            throw new IllegalArgumentException("초기 포인트는 0 이상이어야 합니다.");
        }
        return new Point(userId, initialAmount);
    }

    public void add(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("추가할 포인트는 0보다 커야 합니다.");
        }
        this.amount += amount;
    }

    public void deduct(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("차감할 포인트는 0보다 커야 합니다.");
        }
        if (this.amount < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.amount -= amount;
    }
}
