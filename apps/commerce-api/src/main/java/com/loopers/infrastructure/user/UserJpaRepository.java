package com.loopers.infrastructure.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.user.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);
}
