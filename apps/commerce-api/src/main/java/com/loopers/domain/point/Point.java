package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "point")
@Getter
public class Point extends BaseEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private String userId;

    private Long balance;

    protected Point() {}

    private Point(String userId, Long balance) {
        this.userId = requireValidUserId(userId);
        this.balance = balance;
    }

    public static Point create(String userId, Long balance) {
        return new Point(userId, balance);
    }

    String requireValidUserId(String userId) {
        if(userId == null || userId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID가 비어있을 수 없습니다.");
        }
        return userId;
    }

    public void charge(Long chargeAmount) {
        if (chargeAmount == null || chargeAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0원 이하로 포인트를 충전 할수 없습니다.");
        }
        this.balance += chargeAmount;
        new Point(this.userId, this.balance);
    }

    public void use(Long useAmount) {
        if (useAmount == null || useAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "0원 이하로 사용할 수 없습니다.");
        }
        if (this.balance < useAmount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }
        this.balance -= useAmount;
    }
}
