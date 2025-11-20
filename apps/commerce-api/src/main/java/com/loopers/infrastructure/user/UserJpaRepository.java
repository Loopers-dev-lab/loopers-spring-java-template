package com.loopers.infrastructure.user;

import com.loopers.domain.user.UserModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserModel, Long> {
    boolean existsByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "select u from UserModel u where u.userId = :userId")
    Optional<UserModel> findByUserIdForUpdate(@Param("userId") String userId);

    Optional<UserModel> findByUserId(String userId);

    int deleteByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "select u from UserModel u where u.id = :id")
    Optional<UserModel> findByIdForUpdate(@Param("id") Long id);
}
