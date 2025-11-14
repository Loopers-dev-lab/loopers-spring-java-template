package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_like",
    // 💡 UNIQUE 인덱스를 @Index로 정의하는 것이 더 명확하고 유연할 수 있습니다.
    indexes = {
        @Index(
            name = "uk_user_like_target", 
            columnList = "user_id, like_target_id, like_target_type", 
            unique = true // 명시적으로 UNIQUE 지정
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Like extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "like_target_id")
    private Long likeTargetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "like_target_type")
    private LikeTargetType likeTargetType;

    @Builder
    private Like(
        User user
        , Long likeTargetId
        , LikeTargetType likeTargetType
    ) {
        this.user = user;
        this.likeTargetId = likeTargetId;
        this.likeTargetType = likeTargetType;
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
        if(likeTargetId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId가 비어있을 수 없습니다.");
        } else if(likeTargetId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetId는 양수여야 합니다.");
        }

        // likeTargetType 유효성 검사
        if(likeTargetType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Like : likeTargetType이 비어있을 수 없습니다.");
        }
    }
}

