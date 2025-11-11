package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointTransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal balanceAfter;

    private String description;

    @Builder
    private PointHistory(
        String userId,
        PointTransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description
    ) {
        validateUserId(userId);
        validateTransactionType(transactionType);
        validateAmount(amount);
        validateBalanceAfter(balanceAfter);

        this.userId = userId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
    }

    private void validateTransactionType(PointTransactionType transactionType) {
        if (transactionType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "거래 유형은 필수입니다.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "거래 금액은 0보다 커야 합니다.");
        }
    }

    private void validateBalanceAfter(BigDecimal balanceAfter) {
        if (balanceAfter == null || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                "거래 후 잔액은 0 이상이어야 합니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PointHistory that = (PointHistory) o;
        return Objects.equals(getUserId(), that.getUserId())
            && getTransactionType() == that.getTransactionType()
            && getAmount().compareTo(that.getAmount()) == 0
            && getBalanceAfter().compareTo(that.getBalanceAfter()) == 0
            && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getTransactionType(),
            getAmount().stripTrailingZeros(),
            getBalanceAfter().stripTrailingZeros(),
            getDescription());
    }
}
