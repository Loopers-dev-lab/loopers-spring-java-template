package com.loopers.infrastructure.useraction;

import com.loopers.domain.useraction.UserAction;
import com.loopers.domain.useraction.UserActionRepository;
import com.loopers.domain.useraction.UserActionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 사용자 행동 저장소 구현
 */
@Repository
@RequiredArgsConstructor
public class UserActionRepositoryImpl implements UserActionRepository {

    private final UserActionJpaRepository jpaRepository;

    @Override
    public UserAction save(UserAction userAction) {
        return jpaRepository.save(userAction);
    }

    @Override
    public Page<UserAction> findByUserId(String userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<UserAction> findByUserIdAndActionType(String userId, UserActionType actionType,
        Pageable pageable) {
        return jpaRepository.findByUserIdAndActionType(userId, actionType, pageable);
    }
}
