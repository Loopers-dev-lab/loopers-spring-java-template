package com.loopers.domain.point;

import java.util.Optional;

public interface PointRepository {
    /**
 * Persist the given Point entity.
 *
 * @param point the Point to persist
 * @return the persisted Point instance, possibly containing persistence-generated changes (for example an assigned id or updated timestamps)
 */
Point save(Point point);

    /**
 * Finds the Point associated with the specified user identifier.
 *
 * @param userId the identifier of the user whose Point to retrieve
 * @return an Optional containing the Point for the given userId, or an empty Optional if none exists
 */
Optional<Point> findByUserId(String userId);
}