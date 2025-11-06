package com.loopers.domain.point;

import java.util.List;

import com.loopers.domain.user.UserEntity;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface PointHistoryRepository {
    PointHistoryEntity save(PointHistoryEntity pointHistory);

    /**
     * 특정 사용자의 포인트 이력을 생성일 기준 내림차순으로 조회합니다.
     *
     * @param user 사용자 엔티티
     * @return 포인트 이력 목록 (최신순)
     */
    List<PointHistoryEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
}
