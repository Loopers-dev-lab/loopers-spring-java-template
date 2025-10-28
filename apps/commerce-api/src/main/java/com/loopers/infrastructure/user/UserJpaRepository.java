package com.loopers.infrastructure.user;

import com.loopers.domain.example.ExampleModel;
import com.loopers.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {
}
