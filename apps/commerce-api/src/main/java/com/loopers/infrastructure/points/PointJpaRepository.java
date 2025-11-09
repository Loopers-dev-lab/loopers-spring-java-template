package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<PointModel, Long> {
    /**
 * Finds the PointModel associated with the given member identifier.
 *
 * @param memberId the member's identifier used to locate their points
 * @return an Optional containing the PointModel for the specified memberId if found, or an empty Optional otherwise
 */
Optional<PointModel> findByMemberId(String memberId);
}