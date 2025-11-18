package com.loopers.domain.like;

import com.loopers.domain.like.repository.LikeRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryLikeRepository implements LikeRepository {

    private final Map<String, Like> store = new HashMap<>();

    private String key(String memberId, Long productId) {
        return memberId + ":" + productId;
    }

    @Override
    public Optional<Like> findByMemberIdAndProductId(String memberId, Long productId) {
        return Optional.ofNullable(store.get(key(memberId, productId)));
    }

    @Override
    public boolean existsByMemberIdAndProductId(String memberId, Long productId) {
        return store.containsKey(key(memberId, productId));
    }

    @Override
    public long countByProductId(Long productId) {
        return store.values().stream()
                .mapToLong(like -> like.getProductId().equals(productId) ? 1 : 0)
                .sum();
    }

    @Override
    public Like save(Like like) {
        store.put(key(like.getMemberId(), like.getProductId()), like);
        return like;
    }

    @Override
    public void deleteByMemberIdAndProductId(String memberId, Long productId) {
        store.remove(key(memberId, productId));
    }

    @Override
    public java.util.Set<Long> findLikedProductIds(String memberId, java.util.List<Long> productIds) {
        return productIds.stream()
                .filter(productId -> existsByMemberIdAndProductId(memberId, productId))
                .collect(java.util.stream.Collectors.toSet());
    }

    public void clear() {
        store.clear();
    }
}