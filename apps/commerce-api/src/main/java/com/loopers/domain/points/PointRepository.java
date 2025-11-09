package com.loopers.domain.points;

import java.util.Optional;

public interface PointRepository {
    /**
 * Retrieve the PointModel associated with the given member identifier.
 *
 * @param memberId the member's unique identifier
 * @return an Optional containing the matching PointModel if found, or an empty Optional otherwise
 */
Optional<PointModel> findByMemberId(String memberId);
    /**
 * Persist the given PointModel and return the stored instance.
 *
 * @param pointModel the PointModel to persist (new or updated)
 * @return the persisted PointModel, potentially with updated state such as generated identifiers or timestamps
 */
PointModel save(PointModel pointModel);
}