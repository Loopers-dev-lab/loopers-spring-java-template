package com.loopers.core.infra.database.mysql.user.impl;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.mysql.user.UserPointJpaRepository;
import com.loopers.core.infra.database.mysql.user.entity.UserPointEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointJpaRepository repository;

    @Override
    public UserPoint save(UserPoint userPoint) {
        return repository.save(UserPointEntity.from(userPoint)).to();
    }

    @Override
    public UserPoint getByUserId(UserId userId) {
        return repository.findByUserId(Long.parseLong(userId.value()))
                .orElseThrow(() -> NotFoundException.withName("사용자 포인트"))
                .to();
    }

    @Override
    public Optional<UserPoint> findByUserId(UserId userId) {
        return repository.findByUserId(Long.parseLong(userId.value()))
                .map(UserPointEntity::to);
    }
}
