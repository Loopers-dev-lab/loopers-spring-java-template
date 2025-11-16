package com.loopers.core.domain.user.repository;

import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.vo.UserId;

public interface UserPointRepository {

    UserPoint save(UserPoint userPoint);

    UserPoint getByUserId(UserId userId);

    UserPoint getByUserIdWithLock(UserId userId);
}
