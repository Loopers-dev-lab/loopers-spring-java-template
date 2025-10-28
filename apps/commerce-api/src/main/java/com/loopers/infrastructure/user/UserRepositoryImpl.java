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
    public UserModel save(UserModel userModel) {
        // Spy 활용시 로그 확인용
        System.out.println("저장되는 UserModel 객체 정보 = " + userModel);

        return userJpaRepository.save(userModel);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }
}
