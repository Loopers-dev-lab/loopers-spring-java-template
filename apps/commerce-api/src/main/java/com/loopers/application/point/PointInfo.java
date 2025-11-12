package com.loopers.application.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.point.Point;

public record PointInfo(Long id, UserModel user, Point point) {
    public static PointInfo from(PointModel model) {
        return new PointInfo(model.getId(), model.getUser(), model.getPoint());
    }
    public Point getPoint() {
        return point;
    }
}
