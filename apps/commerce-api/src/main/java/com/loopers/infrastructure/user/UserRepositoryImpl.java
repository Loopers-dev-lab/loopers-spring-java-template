package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public boolean existsByUserId(String id) {
        return userJpaRepository.existsByUserId(id);
    }

    @Override
    public void save(UserModel userModel) {
        userJpaRepository.save(userModel);
    }

    @Override
    public Optional<UserModel> find(String id) {
        return userJpaRepository.findByUserId(id);
    }
}
