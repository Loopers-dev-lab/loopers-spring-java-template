package com.loopers.domain.point;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Getter;

@Getter
@Table(name = "point")
@Entity
public class PointModel extends BaseEntity {
    private static final long INITIAL_POINT = 0L;
    private static final long MINIMUM_CHARGE_POINT = 0L;

    private Long point;

    @Column(name = "user_id", nullable = false)
    private Long userModelId;

    public static PointModel create(final Long userModelId) {
        PointModel pointModel = new PointModel();
        pointModel.point = INITIAL_POINT;
        pointModel.userModelId = userModelId;
        return pointModel;
    }

    public Long charge(Long chargePoint) {
        if (Objects.isNull(chargePoint) || chargePoint <= MINIMUM_CHARGE_POINT) {
            throw new CoreException(ErrorType.BAD_REQUEST, MINIMUM_CHARGE_POINT +" 초과의 포인트만 충전이 가능합니다.");
        }
        this.point += chargePoint;
        return point;
    }
}
