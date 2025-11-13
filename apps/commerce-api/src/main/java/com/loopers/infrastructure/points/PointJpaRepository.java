package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
    Optional<PointModel> findByMemberId(String memberId);
}