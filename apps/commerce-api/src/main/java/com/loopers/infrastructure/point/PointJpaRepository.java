package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.user.UserModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.awt.*;
import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
  boolean existsByUserUserId(String userId);

  Optional<PointModel> findByUserUserId(String userId);
}
