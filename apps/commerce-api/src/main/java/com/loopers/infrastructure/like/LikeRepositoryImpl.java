package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeRepositoryImpl implements LikeRepository {
    private final LikeJpaRepository likeJpaRepository;

    @Override
    public boolean existsByUserIdAndProductId(final Long userId, final Long productId) {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public int saveIgnore(final Long userId, final Long productId) {
        return likeJpaRepository.saveIgnore(userId, productId);
    }

    @Override
    public Optional<Like> findByUserIdAndProductId(final Long userId, final Long productId) {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId);
    }

    @Override
    public void delete(final Like like) {
        likeJpaRepository.delete(like);
    }
}
