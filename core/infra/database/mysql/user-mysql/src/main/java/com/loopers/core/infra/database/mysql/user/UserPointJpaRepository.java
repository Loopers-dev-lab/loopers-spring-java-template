package com.loopers.core.infra.database.mysql.user;

import com.loopers.core.infra.database.mysql.user.entity.UserPointEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserPointJpaRepository extends JpaRepository<UserPointEntity, Long> {

    Optional<UserPointEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserPointEntity up where up.userId = :userId")
    Optional<UserPointEntity> findByUserIdWithLock(Long userId);
}
