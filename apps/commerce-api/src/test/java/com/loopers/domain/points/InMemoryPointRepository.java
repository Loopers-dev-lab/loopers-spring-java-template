package com.loopers.domain.points;

import com.loopers.domain.points.repository.PointRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryPointRepository implements PointRepository {

    private final Map<String, Point> store = new HashMap<>();

    @Override
    public Optional<Point> findByMemberId(String memberId) {
        return Optional.ofNullable(store.get(memberId));
    }

    @Override
    public Optional<Point> findByMemberIdForUpdate(String memberId) {
        return findByMemberId(memberId);
    }

    @Override
    public Point save(Point point) {
        store.put(point.getMemberId(), point);
        return point;
    }

    public void clear() {
        store.clear();
    }
}
