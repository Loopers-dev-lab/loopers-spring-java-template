package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
  UserModel save(UserModel user);

  boolean existsByUserId(String userId);
  
  Optional<UserModel> findByUserId(String userId);
}
