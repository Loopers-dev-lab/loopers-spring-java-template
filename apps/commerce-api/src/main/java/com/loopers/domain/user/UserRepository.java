package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {

    boolean existsByUserId(String userId);

    UserModel save(UserModel userModel);

    Optional<UserModel> findUserByUserId(String userId);
}
