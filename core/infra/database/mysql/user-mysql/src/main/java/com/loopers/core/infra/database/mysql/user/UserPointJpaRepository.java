package com.loopers.core.infra.database.mysql.user;

import com.loopers.core.infra.database.mysql.user.entity.UserPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPointJpaRepository extends JpaRepository<UserPointEntity, Long> {

    Optional<UserPointEntity> findByUserId(Long userId);
}
