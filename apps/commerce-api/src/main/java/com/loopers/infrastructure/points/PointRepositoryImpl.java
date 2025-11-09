package com.loopers.infrastructure.points;

import com.loopers.domain.points.PointModel;
import com.loopers.domain.points.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository pointJpaRepository;

    /**
     * Retrieve the PointModel for the given member identifier.
     *
     * @param memberId the member's unique identifier
     * @return an Optional containing the member's PointModel if found, otherwise an empty Optional
     */
    @Override
    public Optional<PointModel> findByMemberId(String memberId) {
        return pointJpaRepository.findByMemberId(memberId);
    }

    /**
     * Persists the given point model and returns the stored entity.
     *
     * @param pointModel the point model to save
     * @return the saved PointModel instance, potentially with updated persistence state (for example, generated identifiers)
     */
    @Override
    public PointModel save(PointModel pointModel) {
        return pointJpaRepository.save(pointModel);
    }
}