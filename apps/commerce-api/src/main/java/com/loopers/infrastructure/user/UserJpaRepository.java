package com.loopers.infrastructure.user;

import com.loopers.domain.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * User 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능과 
 * 사용자 ID 기반 조회 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface UserJpaRepository extends JpaRepository<User, Long> {
    /**
     * 사용자 ID로 사용자를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자를 담은 Optional
     */
    Optional<User> findByUserId(String userId);

    /**
     * 사용자 ID로 사용자를 조회합니다. (비관적 락)
     * <p>
     * SELECT ... FOR UPDATE를 사용하여 동시성 제어를 보장합니다.
     * </p>
     * <p>
     * <b>Lock 전략:</b>
     * <ul>
     *   <li><b>PESSIMISTIC_WRITE 선택 근거:</b> 포인트 차감 시 Lost Update 방지</li>
     *   <li><b>Lock 범위 최소화:</b> UNIQUE(userId) 인덱스 기반 조회로 해당 행만 락</li>
     *   <li><b>인덱스 활용:</b> UNIQUE 제약조건으로 인덱스가 자동 생성되어 Lock 범위 최소화</li>
     * </ul>
     * </p>
     * <p>
     * <b>동작 원리:</b>
     * <ol>
     *   <li>SELECT ... FOR UPDATE 실행 → 해당 행에 배타적 락 설정</li>
     *   <li>다른 트랜잭션의 쓰기/FOR UPDATE는 차단 (일반 읽기는 가능)</li>
     *   <li>포인트 차감 후 트랜잭션 커밋 → 락 해제</li>
     *   <li>대기 중이던 트랜잭션이 최신 값을 읽어 처리</li>
     * </ol>
     * </p>
     *
     * @param userId 조회할 사용자 ID
     * @return 조회된 사용자를 담은 Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.userId = :userId")
    Optional<User> findByUserIdForUpdate(@Param("userId") String userId);
}
