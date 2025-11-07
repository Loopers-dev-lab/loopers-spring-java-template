package com.loopers.domain.user;

import java.util.Optional;

public interface UserService {
    User register(User user);

    Optional<User> findByLoginId(String loginId);

    User findById(Long id);
}
