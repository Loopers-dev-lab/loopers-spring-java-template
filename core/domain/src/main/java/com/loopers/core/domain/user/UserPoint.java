package com.loopers.core.domain.user;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserPointBalance;
import com.loopers.core.domain.user.vo.UserPointId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UserPoint {

    private final UserPointId id;

    private final UserId userId;

    private final UserPointBalance balance;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private UserPoint(
            UserPointId id,
            UserId userId,
            UserPointBalance balance,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserPoint create(UserId userId) {
        return new UserPoint(
                UserPointId.empty(),
                userId,
                UserPointBalance.init(),
                CreatedAt.now(),
                UpdatedAt.now()
        );
    }

    public static UserPoint mappedBy(
            UserPointId id,
            UserId userId,
            UserPointBalance balance,
            CreatedAt createdAt,
            UpdatedAt updatedAt
    ) {
        return new UserPoint(id, userId, balance, createdAt, updatedAt);
    }

    public UserPoint charge(BigDecimal point) {
        return this.toBuilder()
                .balance(this.balance.add(point))
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public UserPoint pay(PayAmount payAmount) {
        return this.toBuilder()
                .balance(this.balance.decrease(payAmount))
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
