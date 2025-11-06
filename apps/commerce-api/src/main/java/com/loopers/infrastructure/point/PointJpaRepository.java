package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
  boolean existsByUserUserId(String userId);

  Optional<PointModel> findByUserUserId(String userId);

  @Query("SELECT p FROM PointModel p WHERE p.user.userId = :userId")
  Optional<PointModel> findByUserUserIdForUpdate(@Param("userId") String userId);

}
