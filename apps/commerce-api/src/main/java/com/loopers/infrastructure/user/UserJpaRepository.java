package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * User 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능과 
 * 사용자 ID 기반 조회 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {
    /**
 * Finds a user by their unique userId.
 *
 * @param userId the user's unique identifier
 * @return an Optional containing the User if found, otherwise an empty Optional
 */
    Optional<User> findByUserId(String userId);

    /**
     * Retrieve a user by userId while acquiring a pessimistic write lock on the matched row.
     *
     * @param userId the unique user identifier to query
     * @return an Optional containing the User if found, or Optional.empty() otherwise
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByUserIdForUpdate(@Param("userId") String userId);
}