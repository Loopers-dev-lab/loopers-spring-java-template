package com.loopers.domain.point;

/**
 * Point 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 포인트 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface PointRepository {
    /**
     * 포인트를 저장합니다.
     *
     * @param point 저장할 포인트
     * @return 저장된 포인트
     */
    Point save(Point point);
    
    /**
     * 사용자 ID로 포인트를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 포인트, 없으면 null
     */
    Point findByUserId(String userId);
}


