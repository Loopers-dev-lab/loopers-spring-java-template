package com.loopers.domain.point;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Point s WHERE s.id= :id")
    Optional<Point> findByUserId(@Param("id") String userId);

    Point save(Point point);
}
