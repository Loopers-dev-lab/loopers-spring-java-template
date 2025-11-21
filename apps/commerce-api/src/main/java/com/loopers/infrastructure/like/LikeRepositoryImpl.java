package com.loopers.infrastructure.like;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.entity.Like;
import com.loopers.domain.like.entity.LikeTargetType;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;
    private final LikeQueryRepository likeQueryRepository;

    @Override
    public Optional<Like> save(Like like) {
        Like savedLike = likeJpaRepository.save(like);
        return Optional.of(savedLike);
    }

    @Override
    public long deleteByUserIdAndLikeTargetId(Long userId, Long likeTargetId, LikeTargetType likeTargetType) {
        return likeQueryRepository.deleteByUserIdAndLikeTargetId(userId, likeTargetId, likeTargetType.name());
    }

    @Override
    public Optional<Like> findByUserIdAndLikeTargetId(Long userId, Long likeTargetId, LikeTargetType likeTargetType) {
        return likeJpaRepository.findByUser_IdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(userId, likeTargetId, likeTargetType);
    }

    @Override
    public long insertOrIgnore(Long userId, Long likeTargetId, LikeTargetType likeTargetType) {
        return likeQueryRepository.insertOrIgnore(userId, likeTargetId, likeTargetType.name());
    }
}

