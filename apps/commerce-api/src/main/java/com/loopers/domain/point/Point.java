package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "point")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Point extends BaseEntity {

    private BigDecimal amount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    private Point(
            BigDecimal amount
            , User user
    ) {
        this.amount = amount;
        this.user = user;
        guard();
    }

    // User 필드를 세팅
    public void setUser(User user) {
        if(this.user != null) return;
        this.user = user;
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // amount : Null 혹은 음수인지 검사
        if(amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Point : amount가 Null 이 되면 안 됩니다.");
        } else if(amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Point : amount는 음수가 될 수 없습니다.");
        }

        // user : Null 인지 검사
//        if(user == null) {
//            throw new CoreException(ErrorType.BAD_REQUEST, "Point : user가 Null 이 되면 안 됩니다.");
//        }
    }

    public BigDecimal charge(BigDecimal amount) {
        // 충전하는 포인트가 음수이면 BAD REQUEST CoreException 을 발생
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 point는 0 이하가 될 수 없습니다.");
        }
        this.amount = this.amount.add(amount);
        return this.amount;
    }

    public BigDecimal deduct(BigDecimal amount) {
        // 차감하는 포인트가 음수이면 BAD REQUEST CoreException 을 발생
        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 point는 0 이하가 될 수 없습니다.");
        }
        
        // 포인트가 부족하면 BAD REQUEST CoreException 을 발생
        if(this.amount.compareTo(amount) < 0) {
            throw new CoreException(
                ErrorType.BAD_REQUEST, 
                "포인트가 부족합니다. (현재 포인트: " + this.amount + ", 요청 금액: " + amount + ")"
            );
        }
        
        this.amount = this.amount.subtract(amount);
        return this.amount;
    }
}
