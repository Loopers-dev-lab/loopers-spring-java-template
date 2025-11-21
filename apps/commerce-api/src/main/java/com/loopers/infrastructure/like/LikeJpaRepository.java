package com.loopers.infrastructure.like;

import com.loopers.domain.like.entity.Like;
import com.loopers.domain.like.entity.LikeId;
import com.loopers.domain.like.entity.LikeTargetType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, LikeId> {
    Optional<Like> findByUser_IdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
    void deleteByUser_IdAndLikeId_LikeTargetIdAndLikeId_LikeTargetType(Long userId, Long likeTargetId, LikeTargetType likeTargetType);
}

