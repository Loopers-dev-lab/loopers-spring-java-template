package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<User> findByUserIdForUpdate(String userId) {
        return userJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public boolean existsUserByUserId(String userId) {
        return userJpaRepository.existsUserByUserId(userId);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }
}
