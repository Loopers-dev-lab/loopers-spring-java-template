package com.loopers.infrastructure.point;

import com.loopers.domain.points.PointsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointsModel, Long> {
    
    @Query("SELECT COUNT(p) > 0 FROM PointsModel p WHERE p.userId.userId = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT p FROM PointsModel p WHERE p.userId.userId = :userId")
    Optional<PointsModel> findByUserId(@Param("userId") Long userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointsModel p WHERE p.userId.userId = :userId")
    Optional<PointsModel> findByUserIdForUpdate(@Param("userId") Long userId);
}
