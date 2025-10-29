package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
  private final UserJpaRepository jpaRepository;

  @Override
  public UserModel save(UserModel user) {
    return jpaRepository.save(user);
  }

  @Override
  public boolean existsByUserId(String userId) {
    return jpaRepository.existsByUserId(userId);
  }
  @Override
  public Optional<UserModel> findByUserId(String userId) {
    return jpaRepository.findByUserId(userId);
  }
}
