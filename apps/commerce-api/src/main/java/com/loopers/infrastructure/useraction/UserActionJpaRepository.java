package com.loopers.infrastructure.useraction;

import com.loopers.domain.useraction.UserAction;
import com.loopers.domain.useraction.UserActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 행동 JPA 저장소
 */
public interface UserActionJpaRepository extends JpaRepository<UserAction, Long> {
    Page<UserAction> findByUserId(String userId, Pageable pageable);

    Page<UserAction> findByUserIdAndActionType(String userId, UserActionType actionType,
        Pageable pageable);
}
