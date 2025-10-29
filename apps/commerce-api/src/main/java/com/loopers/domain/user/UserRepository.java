package com.loopers.domain.user;

public interface UserRepository {

    boolean existsByUserId(String userId);

    UserModel save(UserModel userModel);
}
