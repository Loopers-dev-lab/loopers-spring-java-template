package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Point 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능과 
 * 사용자 ID 기반 조회 기능을 제공합니다.
 * N+1 문제 방지를 위해 Fetch Join을 사용합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface PointJpaRepository extends JpaRepository<Point, Long> {
    /**
     * 사용자 ID로 포인트를 조회합니다.
     * <p>
     * JOIN FETCH를 사용하여 연관된 User 엔티티를 함께 로드합니다.
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 포인트를 담은 Optional
     */
    @Query("SELECT p FROM Point p JOIN FETCH p.user WHERE p.user.userId = :userId")
    Optional<Point> findByUserId(@Param("userId") String userId);
}
