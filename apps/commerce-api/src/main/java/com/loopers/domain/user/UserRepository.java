package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUserId(String id);

    User save(User user);

    Optional<User> find(String id);
}
