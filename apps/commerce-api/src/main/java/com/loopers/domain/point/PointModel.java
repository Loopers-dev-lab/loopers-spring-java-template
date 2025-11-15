package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.Money;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;


@Entity
@Table(name = "point")
public class PointModel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_model_id")
    private UserModel user;
    @Embedded
    private Money point;

    public PointModel() {
    }

    public PointModel(UserModel user, Money point) {

        this.user = user;
        this.point = point;
    }

    public UserModel getUser() {
        return user;
    }

    public Money getPoint() {
        return point;
    }

    public void charge(Money chargePoint) {
        long newPointValue = this.point.value() + chargePoint.value();
        this.point = new Money(newPointValue);
    }

    public void use(Money usePoint) {
        if (this.point.value() < usePoint.value()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }

        if (usePoint.value() > this.point.value()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 금액이 보유 포인트를 초과합니다.");
        }

        long newPointValue = this.point.value() - usePoint.value();
        this.point = new Money(newPointValue);

    }
}
