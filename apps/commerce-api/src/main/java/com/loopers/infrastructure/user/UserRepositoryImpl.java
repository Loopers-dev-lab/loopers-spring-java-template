package com.loopers.infrastructure.user;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.example.ExampleRepository;
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
    return jpaRepository.save(user);
  }

  @Override
  public Optional<User> find(Long id) {
    return jpaRepository.findById(id);
  }
}
