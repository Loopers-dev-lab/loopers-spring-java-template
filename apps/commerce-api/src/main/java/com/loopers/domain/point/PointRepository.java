package com.loopers.domain.point;

import com.loopers.domain.user.UserModel;

import java.util.Optional;

public interface PointRepository {
    Optional<PointModel> findPoint(UserModel user);
    PointModel save(PointModel pointModel);
}
