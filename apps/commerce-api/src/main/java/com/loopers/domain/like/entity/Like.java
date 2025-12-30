package com.loopers.domain.like.entity;

import com.loopers.domain.AuditEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Like extends AuditEntity {

    @EmbeddedId
    private LikeId likeId;

    @Builder
    private Like(
        Long userId
        , Long likeTargetId
        , LikeTargetType likeTargetType
    ) {
        this.likeId = LikeId.builder()
            .userId(userId)
            .likeTargetId(likeTargetId)
            .likeTargetType(likeTargetType)
            .build();
        guard();
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // likeId 유효성 검사
        if(likeId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeId가 비어있을 수 없습니다.");
        }

        // userId 유효성 검사
        if(likeId.getUserId() == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : userId가 비어있을 수 없습니다.");
        } else if(likeId.getUserId() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : userId는 양수여야 합니다.");
        }

        // likeTargetId 유효성 검사
        if(likeId.getLikeTargetId() == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId가 비어있을 수 없습니다.");
        } else if(likeId.getLikeTargetId() <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId는 양수여야 합니다.");
        }

        // likeTargetType 유효성 검사
        if(likeId.getLikeTargetType() == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetType이 비어있을 수 없습니다.");
        }
    }
}

