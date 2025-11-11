package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUserId(String userId);

    Optional<User> findByUserIdForUpdate(String userId);

    boolean existsUserByUserId(String userId);

    User save(User user);
}
