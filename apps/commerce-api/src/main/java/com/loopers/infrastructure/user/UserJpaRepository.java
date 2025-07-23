package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.embeded.UserLoginId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    List<UserEntity> findByLoginId(String loginId);

    boolean existsByLoginId(UserLoginId loginId);
}
