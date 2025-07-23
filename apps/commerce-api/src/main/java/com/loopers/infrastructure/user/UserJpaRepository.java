package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.embeded.UserLoginId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByLoginId(UserLoginId loginId);
}
