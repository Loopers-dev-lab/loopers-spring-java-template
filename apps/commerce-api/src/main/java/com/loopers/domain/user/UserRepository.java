package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUserId(String id);

    UserModel save(UserModel userModel);

    Optional<UserModel> find(String id);
}
