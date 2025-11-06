package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    UserModel save(UserModel model);

    boolean existsByUserId(String userId);

    Optional<UserModel> find(Long id);
}
