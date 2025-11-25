package com.loopers.domain.user;

import java.util.Optional;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
public interface UserRepository {
    UserEntity save(UserEntity userEntity);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameWithLock(String username);
}
