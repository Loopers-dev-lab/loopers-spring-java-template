package com.loopers.domain.user;

public interface UserRepository {
    UserModel save(UserModel userModel);

    boolean existsByUserId(String userId);
}
