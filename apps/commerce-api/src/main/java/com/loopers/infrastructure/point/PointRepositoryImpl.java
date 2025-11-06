package com.loopers.infrastructure.point;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointRepositoryImpl implements PointRepository {
    private final PointJpaRepository repository;

    @Override
    public Optional<PointModel> findByUserModelId(final Long userModelId) {
        return repository.findByUserModelId(userModelId);
    }

    @Override
    public PointModel save(final PointModel pointModel) {
        return repository.save(pointModel);
    }
}
