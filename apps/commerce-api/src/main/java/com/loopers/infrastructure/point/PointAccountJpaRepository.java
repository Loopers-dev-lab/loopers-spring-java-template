package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointAccountJpaRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(String userId);
}
