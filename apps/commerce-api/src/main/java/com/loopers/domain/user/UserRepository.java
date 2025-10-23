package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<UserModel> findById(String id);
    UserModel save(UserModel user);
    boolean existsById(String id);
}
