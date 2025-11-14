package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    boolean existsUserId(String userId);
    Optional<UserModel> findByUserId(String userId);
    UserModel save(UserModel user);
    boolean deleteUser(String userId);
    Optional<UserModel> findById(Long id);
}
