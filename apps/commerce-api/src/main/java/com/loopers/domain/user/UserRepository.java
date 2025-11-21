package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
  User save(User user);

  Optional<User> findByUserId(String userId);

  long findPointById(Long userId);

  Optional<User> findById(Long id);

  Optional<User> findByUserIdWithLock(Long id);
}
