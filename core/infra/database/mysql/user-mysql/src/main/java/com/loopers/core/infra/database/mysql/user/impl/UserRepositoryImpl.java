package com.loopers.core.infra.database.mysql.user.impl;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.infra.database.mysql.user.UserJpaRepository;
import com.loopers.core.infra.database.mysql.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository repository;

    @Override
    public User save(User user) {
        return repository.save(UserEntity.from(user)).to();
    }
}
