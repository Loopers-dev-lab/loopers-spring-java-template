package com.loopers.infrastructure.user;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    @Override
    public UserEntity save(UserEntity userEntity) {
        return userJpaRepository.save(userEntity);
    }

    /**
     * Finds a non-deleted user by their username.
     *
     * @param username the username of the user to find
     * @return an Optional containing the matching UserEntity if found and not deleted, or an empty Optional otherwise
     */
    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userJpaRepository.findByUsernameAndDeletedAtIsNull(username);
    }

    /**
     * Finds a non-deleted user by username and acquires a database lock on the matching row.
     *
     * @param username the username to search for
     * @return an Optional containing the matching UserEntity if present, Optional.empty() otherwise
     */
    @Override
    public Optional<UserEntity> findByUsernameWithLock(String username) {
        return userJpaRepository.findByUsernameWithLockAndDeletedAtIsNull(username);
    }
}