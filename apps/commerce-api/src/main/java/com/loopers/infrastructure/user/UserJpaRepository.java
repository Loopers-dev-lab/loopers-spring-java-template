package com.loopers.infrastructure.user;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.user.UserEntity;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    /**
 * Finds a non-deleted user by username.
 *
 * @param username the username to search for
 * @return an Optional containing the matching UserEntity if found, `Optional.empty()` otherwise
 */
Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);

    /**
     * Finds a non-deleted user by username and acquires a pessimistic write lock on the matching row.
     *
     * @param username the username to search for
     * @return an Optional containing the matching UserEntity if present and not deleted, or Optional.empty() otherwise
     */
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<UserEntity> findByUsernameWithLockAndDeletedAtIsNull(String username);
}