package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "point")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Point extends BaseEntity {
    @Column(name = "ref_user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long balance;

    public Point(Long userId, Long balance) {
        PointValidator.validateBalance(balance);

        this.userId = userId;
        this.balance = balance;
    }

    public void charge(Long amount) {
        PointValidator.validateChargeAmount(amount); // 충전 금액 유효성 검사 (0보다 커야 함)
        this.balance += amount;
    }

}
