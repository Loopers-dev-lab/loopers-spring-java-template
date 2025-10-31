package com.loopers.infrastructure.point;

import com.loopers.domain.point.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointJpaRepository extends JpaRepository<Point, String> {
    /**
 * Finds the Point entity associated with the given user identifier.
 *
 * @param userId the identifier of the user whose Point should be retrieved
 * @return an Optional containing the Point for the specified userId, or an empty Optional if none exists
 */
Optional<Point> findByUserId(String userId);
}