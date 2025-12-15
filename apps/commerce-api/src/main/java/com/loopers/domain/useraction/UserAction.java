package com.loopers.domain.useraction;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 행동 추적 엔티티
 * - 사용자의 행동 로그를 저장하여 추천 시스템, 분석 등에 활용
 */
@Getter
@Entity
@Table(name = "user_actions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAction extends BaseEntity {

    @Column(nullable = false, length = 10)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserActionType actionType;

    @Column(length = 20)
    private String targetId;

    @Column(length = 50)
    private String targetType;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Builder
    private UserAction(String userId, UserActionType actionType, String targetId,
        String targetType, String metadata) {
        validateUserId(userId);
        validateActionType(actionType);

        this.userId = userId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.targetType = targetType;
        this.metadata = metadata;
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
    }

    private void validateActionType(UserActionType actionType) {
        if (actionType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Action Type은 필수입니다.");
        }
    }
}
