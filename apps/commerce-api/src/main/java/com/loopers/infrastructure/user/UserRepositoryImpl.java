package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;

  @Override
  public User save(User user) {
    return jpaRepository.saveAndFlush(user);
  }

  @Override
  public Optional<User> findById(Long userId) {
    return jpaRepository.findById(userId);
  }

  @Override
  public Optional<User> findByLoginId(String loginId) {
    return jpaRepository.findByLoginId(loginId);
  }

}
