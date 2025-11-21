package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<UserModel> find(UserId userId);
    
    Optional<UserModel> findById(Long id);
    UserModel save(UserModel userModel);
}