package com.loopers.domain.point;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
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

    /**
     * Create a new Point entity for a user with a validated initial balance.
     *
     * @param userId       the user identifier; must not be null or blank
     * @param initialAmount the initial point balance; must be greater than or equal to 0
     * @return              a Point initialized with the provided userId and initialAmount
     * @throws CoreException if userId is null or blank, or if initialAmount is null or less than 0 (ErrorType.BAD_REQUEST)
     */
    public static Point create(String userId, Long initialAmount) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 비어있을 수 없습니다.");
        }
        if (initialAmount == null || initialAmount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "초기 포인트는 0 이상이어야 합니다.");
        }
        return new Point(userId, initialAmount);
    }

    /**
     * Increase the point balance by the specified amount.
     *
     * @param amount the number of points to add; must be greater than zero
     * @throws CoreException if {@code amount} is {@code null} or less than or equal to zero
     */
    public void add(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "추가할 포인트는 0보다 커야 합니다.");
        }
        this.amount += amount;
    }

    /**
     * Decreases the current point balance by the specified positive amount.
     *
     * @param amount the number of points to deduct; must be greater than zero
     * @throws CoreException if {@code amount} is null or less than or equal to zero, or if the current balance is less than {@code amount}
     */
    public void deduct(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 포인트는 0보다 커야 합니다.");
        }
        if (this.amount < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }
        this.amount -= amount;
    }
}