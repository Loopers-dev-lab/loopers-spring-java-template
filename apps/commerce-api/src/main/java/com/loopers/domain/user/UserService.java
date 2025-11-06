package com.loopers.domain.user;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@Component
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserEntity register(@Valid UserDomainCreateRequest userRegisterRequest) {
        userRepository.findByUsername(userRegisterRequest.username())
                .ifPresent(user -> {
                    throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다: " + userRegisterRequest.username());
                });

        UserEntity userEntity = UserEntity.createUserEntity(userRegisterRequest);

        return userRepository.save(userEntity);
    }

    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }
}
