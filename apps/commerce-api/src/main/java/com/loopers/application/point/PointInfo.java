package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.user.UserModel;

public record PointInfo(Long id, UserModel user, int point) {
    public static PointInfo from(PointModel model) {
        return new PointInfo(model.getId(), model.getUser(), model.getPoint());
    }
    public int getPoint() {
        return point;
    }
}
