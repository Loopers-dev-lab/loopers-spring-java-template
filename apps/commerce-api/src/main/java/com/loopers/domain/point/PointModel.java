package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "point")
public class PointModel extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_model_id")
    private UserModel user;
    private Point point;

    public PointModel() {
    }

    public PointModel(UserModel user, Point point) {

        this.user = user;
        this.point = point;
    }

    public UserModel getUser() {
        return user;
    }

    public Point getPoint() {
        return point;
    }

    public void charge(Point chargePoint) {
        int newPointValue = this.point.point() + chargePoint.point();
        this.point = new Point(newPointValue);
    }

    public void use(Point usePoint) {
        if (this.point.point() < usePoint.point()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }

        if (usePoint.point() > this.point.point()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 금액이 보유 포인트를 초과합니다.");
        }

        int newPointValue = this.point.point() - usePoint.point();
        this.point = new Point(newPointValue);

    }
}
