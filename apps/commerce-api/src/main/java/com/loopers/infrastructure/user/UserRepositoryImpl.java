package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

  @Override
  public User save(User user) {
    return userJpaRepository.save(user);
  }

  @Override
  public Optional<User> findByUserId(String userId) {
    return userJpaRepository.findByUserId(userId);
  }

  @Override
  public long findPointById(Long userId) {
    return userJpaRepository.findPointById(userId);
  }

  @Override
  public Optional<User> findById(Long id) {
    return userJpaRepository.findById(id);
  }

  @Override
  public Optional<User> findByUserIdWithLock(Long id) {
    return userJpaRepository.findByUserIdWithLock(id);
  }
}
