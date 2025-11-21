package com.loopers.domain.like.entity;

import com.loopers.domain.AuditEntity;
import com.loopers.domain.user.User;
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

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    private Like(
        User user
        , Long likeTargetId
        , LikeTargetType likeTargetType
    ) {
        // 입력값 선검증으로 NPE 방지 및 메시지 일관화
        if (user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : user가 비어있을 수 없습니다.");
        }
        if (likeTargetId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId가 비어있을 수 없습니다.");
        }
        if (likeTargetId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId는 양수여야 합니다.");
        }
        if (likeTargetType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetType이 비어있을 수 없습니다.");
        }

        this.user = user;
        this.likeId = LikeId.builder()
            .userId(user.getId())
            .likeTargetId(likeTargetId)
            .likeTargetType(likeTargetType)
            .build();
        guard();
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // user 유효성 검사
        if(user == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : user가 비어있을 수 없습니다.");
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

