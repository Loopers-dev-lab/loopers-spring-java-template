package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    UserEntity save(UserEntity userEntity);

    Optional<UserEntity> findUserByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    Optional<UserEntity> findById(Long id);
}
