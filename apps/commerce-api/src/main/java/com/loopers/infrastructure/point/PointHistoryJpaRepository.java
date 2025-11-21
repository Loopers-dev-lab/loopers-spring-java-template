package com.loopers.infrastructure.point;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.loopers.domain.point.PointHistoryEntity;
import com.loopers.domain.user.UserEntity;

/**
 * @author hyunjikoh
 * @since 2025. 10. 31.
 */
public interface PointHistoryJpaRepository extends JpaRepository<PointHistoryEntity, Long> {

    /**
     * 특정 사용자 포인트의 이력을 생성일 기준 내림차순으로 조회합니다.
     *
     * @param user 사용자 엔티티
     * @return 포인트 이력 목록 (최신순)
     */
    List<PointHistoryEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
}
