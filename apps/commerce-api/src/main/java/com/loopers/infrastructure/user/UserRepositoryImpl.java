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
    public Optional<UserModel> find(String userId) {
         return userJpaRepository.findByUserId(userId);
     }

    @Override
    public Optional<UserModel> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public UserModel save(UserModel userModel) {
        return userJpaRepository.save(userModel);
    }



}
