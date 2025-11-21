package com.loopers.domain.like.repository;

import com.loopers.domain.like.Like;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LikeRepository {
    
    Optional<Like> findByMemberIdAndProductId(String memberId, Long productId);
    
    boolean existsByMemberIdAndProductId(String memberId, Long productId);
    
    long countByProductId(Long productId);
    
    Like save(Like like);
    
    void deleteByMemberIdAndProductId(String memberId, Long productId);
    
    Set<Long> findLikedProductIds(String memberId, List<Long> productIds);
}