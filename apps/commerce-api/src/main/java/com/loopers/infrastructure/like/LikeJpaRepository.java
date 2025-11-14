package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUser_IdAndLikeTargetIdAndLikeTargetType(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
    void deleteByUser_IdAndLikeTargetIdAndLikeTargetType(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
}

