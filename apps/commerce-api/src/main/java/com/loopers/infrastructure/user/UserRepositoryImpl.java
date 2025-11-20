package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }

    @Override
    public Optional<UserModel> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<UserModel> findByUserIdForUpdate(String userId) {
        return userJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public UserModel save(UserModel user) {
        return userJpaRepository.save(user);
    }

    @Override
    public boolean deleteUser(String userId) {
        return userJpaRepository.deleteByUserId(userId) > 0;
    }

    @Override
    public Optional<UserModel> findById(Long pkId) {
        return userJpaRepository.findById(pkId);
    }

    @Override
    public Optional<UserModel> findByIdForUpdate(Long id) {
        return userJpaRepository.findByIdForUpdate(id);
    }


}
