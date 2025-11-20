package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    User save(User model);

    boolean existsByUserId(String userId);

    Optional<User> find(Long id);
}
