package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<UserModel> find(String userId);
    Optional<UserModel> findById(Long id);

    UserModel save(UserModel userModel);
}
