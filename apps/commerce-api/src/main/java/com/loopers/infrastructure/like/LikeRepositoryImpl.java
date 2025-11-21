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
    public Like save(Like like) {
        return likeJpaRepository.save(like);
    }

    @Override
    public List<Like> findActiveLikesByUserId(Long userId) {
        return likeJpaRepository.findActiveLikesByUserId(userId);
    }
}
