package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, Long> {

  @Query("SELECT p FROM Point p WHERE p.userId = :userId")
  Optional<Point> findByUserId(@Param("userId") Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Point p WHERE p.userId = :userId")
  Optional<Point> findByUserIdWithLock(@Param("userId") Long userId);

  @Query("SELECT p FROM Point p JOIN User u ON p.userId = u.id WHERE u.loginId = :loginId")
  Optional<Point> findByUserLoginId(@Param("loginId") String loginId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Point p JOIN User u ON p.userId = u.id WHERE u.loginId = :loginId")
  Optional<Point> findByUserLoginIdWithLock(@Param("loginId") String loginId);

}
