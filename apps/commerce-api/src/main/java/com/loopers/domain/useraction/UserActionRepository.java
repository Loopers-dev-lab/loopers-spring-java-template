package com.loopers.domain.useraction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 행동 저장소
 */
public interface UserActionRepository {
    UserAction save(UserAction userAction);

    Page<UserAction> findByUserId(String userId, Pageable pageable);

    Page<UserAction> findByUserIdAndActionType(String userId, UserActionType actionType,
        Pageable pageable);
}
