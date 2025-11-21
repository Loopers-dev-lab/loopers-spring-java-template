package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class LikeRepositoryImpl implements LikeRepository {
    
    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Optional<Like> findByMemberIdAndProductId(String memberId, Long productId) {
        return likeJpaRepository.findByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public boolean existsByMemberIdAndProductId(String memberId, Long productId) {
        return likeJpaRepository.existsByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public long countByProductId(Long productId) {
        return likeJpaRepository.countByProductId(productId);
    }

    @Override
    public Like save(Like like) {
        return likeJpaRepository.save(like);
    }

    @Override
    public void deleteByMemberIdAndProductId(String memberId, Long productId) {
        likeJpaRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public Set<Long> findLikedProductIds(String memberId, List<Long> productIds) {
        return likeJpaRepository.findByMemberIdAndProductIdIn(memberId, productIds)
                .stream()
                .map(Like::getProductId)
                .collect(Collectors.toSet());
    }
}