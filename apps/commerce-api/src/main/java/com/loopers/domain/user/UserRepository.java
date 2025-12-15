package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
}
