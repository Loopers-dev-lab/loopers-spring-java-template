package com.loopers.core.infra.database.mysql.user.impl;

import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.infra.database.mysql.user.UserPointJpaRepository;
import com.loopers.core.infra.database.mysql.user.entity.UserPointEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointJpaRepository repository;

    @Override
    public UserPoint save(UserPoint userPoint) {
        return repository.save(UserPointEntity.from(userPoint)).to();
    }
}
