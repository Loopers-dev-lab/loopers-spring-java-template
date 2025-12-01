package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Optional<Like> findByUserIdAndProductId(Long userId, Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public boolean save(Like like) {
        int result = likeJpaRepository.insertIgnore(like.getUserId(), like.getProductId());
        return result > 0;
    }

    @Override
    public List<Like> findByUserId(Long userId) {
        return likeJpaRepository.findByUserId(userId);
    }

    @Override
    public boolean delete(Like like) {
        int result = likeJpaRepository.delete(like.getUserId(), like.getProductId());
        return result > 0;
    }
}
