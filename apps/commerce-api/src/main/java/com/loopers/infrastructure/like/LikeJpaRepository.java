package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    
    Optional<Like> findByMemberIdAndProductId(String memberId, Long productId);
    
    boolean existsByMemberIdAndProductId(String memberId, Long productId);
    
    long countByProductId(Long productId);
    
    void deleteByMemberIdAndProductId(String memberId, Long productId);
    
    List<Like> findByMemberIdAndProductIdIn(String memberId, List<Long> productIds);
}