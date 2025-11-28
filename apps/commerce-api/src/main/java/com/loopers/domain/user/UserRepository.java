package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {

    boolean existsByUserId(String userId);

    User save(User user);

    Optional<User> findUserByUserId(String userId);

    Optional<User> findUserByUserIdWithLock(String userId);
}
