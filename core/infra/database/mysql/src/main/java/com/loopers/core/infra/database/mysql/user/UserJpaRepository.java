package com.loopers.core.infra.database.mysql.user;

import com.loopers.core.infra.database.mysql.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByIdentifier(String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.identifier = :identifier")
    Optional<UserEntity> findByIdentifierWithLock(String identifier);
}
