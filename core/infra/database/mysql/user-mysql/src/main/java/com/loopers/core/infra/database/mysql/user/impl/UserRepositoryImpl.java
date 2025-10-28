package com.loopers.core.infra.database.mysql.user.impl;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.infra.database.mysql.user.UserJpaRepository;
import com.loopers.core.infra.database.mysql.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository repository;

    @Override
    public User save(User user) {
        return repository.save(UserEntity.from(user)).to();
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return repository.findById(Long.parseLong(userId.value()))
                .map(UserEntity::to);
    }

    @Override
    public Optional<User> findByIdentifier(UserIdentifier identifier) {
        return repository.findByIdentifier(identifier.value())
                .map(UserEntity::to);
    }
}
