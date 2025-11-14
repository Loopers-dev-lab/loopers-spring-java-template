package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
  User save(User user);

  boolean existsByUserId(String userId);

  Optional<User> findById(Long id);

  Optional<User> findByUserId(String userId);

}
