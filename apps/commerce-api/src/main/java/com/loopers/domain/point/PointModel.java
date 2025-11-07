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
    private int point = 0;

    public PointModel() {}

    public PointModel(UserModel user, int point) {

        if( point < 0 ){
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.");
        }
        this.user = user;
        this.point = point;
    }

    public UserModel getUser() {
        return user;
    }

    public int getPoint() {
        return point;
    }

    public void charge(int amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.");
        }
        this.point += amount;
    }

    public void use(int amount) {

        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");
        }
        if (point < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.");
        }
        this.point -= amount;
    }
}
