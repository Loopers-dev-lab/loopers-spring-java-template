package com.loopers.core.infra.database.mysql.user;

import com.loopers.core.infra.database.mysql.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdentifier(String identifier);
}
