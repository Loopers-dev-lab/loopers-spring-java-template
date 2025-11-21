package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByUserId(String userId);

    User save(User user);
}
