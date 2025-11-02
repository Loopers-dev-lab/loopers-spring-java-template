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
     * 사용자 ID로 사용자를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자, 없으면 null
     */
    User findByUserId(String userId);
}
