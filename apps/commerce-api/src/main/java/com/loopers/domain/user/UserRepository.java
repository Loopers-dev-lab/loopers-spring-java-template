package com.loopers.domain.user;

/**
 * User 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 사용자 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface UserRepository {
    /**
     * 사용자를 저장합니다.
     *
     * @param user 저장할 사용자
     * @return 저장된 사용자
     */
    User save(User user);
    
    /**
 * Retrieve a User by its userId.
 *
 * @param userId the user identifier to look up
 * @return the User with the given userId, or {@code null} if none is found
 */
    User findByUserId(String userId);

    /**
     * 사용자 ID로 사용자를 조회합니다. (비관적 락)
     * <p>
     * 동시성 제어가 필요한 경우 사용합니다. (예: 포인트 차감)
     * </p>
     * <p>
     * <b>Lock 전략:</b>
     * <ul>
     *   <li><b>PESSIMISTIC_WRITE:</b> SELECT ... FOR UPDATE 사용</li>
     *   <li><b>Lock 범위:</b> UNIQUE(userId) 인덱스 기반 조회로 해당 행만 락 (최소화)</li>
     *   <li><b>사용 목적:</b> 포인트 차감 시 Lost Update 방지</li>
     * </ul>
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자, 없으면 null
     */
    User findByUserIdForUpdate(String userId);
}