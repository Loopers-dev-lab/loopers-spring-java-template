package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    boolean existsUserId(String userId);
    Optional<UserModel> findByUserIdForUpdate(String userId);
    UserModel save(UserModel user);
    boolean deleteUser(String userId);
    Optional<UserModel> findByIdForUpdate(Long id);
    Optional<UserModel> findById(Long id);
    Optional<UserModel> findByUserId(String userId);
}
