package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUserId(String id);

    void save(UserModel userModel);

    Optional<UserModel> find(String id);
}
