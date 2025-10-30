package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, String> {
    Optional<User> findById(String id);
}
