package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
  User save(User user);

  Optional<User> find(Long id);
}
