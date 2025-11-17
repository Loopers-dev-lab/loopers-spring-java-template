package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    User save(User user);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

    Optional<User> findByLoginId(String loginId);

}
