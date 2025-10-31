package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 도메인 엔티티.
 * <p>
 * 사용자의 포인트 잔액을 관리하며, 포인트 충전 기능을 제공합니다.
 * User와 일대일 관계를 맺고 있습니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "point")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Point extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "id",
        foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)
    )
    private User user;

    @Column(name = "balance", nullable = false)
    private Long balance;

    /**
     * Point 인스턴스를 생성합니다.
     *
     * @param user 포인트 소유자
     * @param balance 초기 잔액 (null인 경우 0으로 초기화)
     */
    public Point(User user, Long balance) {
        this.user = user;
        this.balance = balance != null ? balance : 0L;
    }

    /**
     * Point 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param user 포인트 소유자
     * @param balance 초기 잔액
     * @return 생성된 Point 인스턴스
     */
    public static Point of(User user, Long balance) {
        return new Point(user, balance);
    }

    /**
     * 포인트를 충전합니다.
     *
     * @param amount 충전할 포인트 금액 (0보다 커야 함)
     * @throws CoreException amount가 null이거나 0 이하일 경우
     */
    public void charge(Long amount) {
        validateChargeAmount(amount);
        this.balance += amount;
    }

    /**
     * 충전 금액의 유효성을 검증합니다.
     *
     * @param amount 검증할 충전 금액
     * @throws CoreException amount가 null이거나 0 이하일 경우
     */
    private void validateChargeAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0보다 큰 값이어야 합니다.");
        }
    }
}


