package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.common.Money;

public record PointInfo(Long id, UserModel user, Money point) {
    public static PointInfo from(PointModel model) {
        return new PointInfo(model.getId(), model.getUser(), model.getPoint());
    }
    public Money getPoint() {
        return point;
    }
}
