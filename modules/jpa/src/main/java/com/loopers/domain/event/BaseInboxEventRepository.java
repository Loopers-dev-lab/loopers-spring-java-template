package com.loopers.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * 도메인별 Inbox Event Repository의 공통 쿼리를 정의하는 인터페이스
 * @NoRepositoryBean을 사용하여 Spring Data JPA가 이 인터페이스 자체를 Repository Bean으로 만들지 않도록 함
 */
@NoRepositoryBean
public interface BaseInboxEventRepository<T extends BaseInboxEvent> extends JpaRepository<T, Long> {

    /**
     * eventId로 처리된 이벤트를 조회합니다.
     * 
     * @param eventId 이벤트 고유 ID
     * @return 처리된 이벤트 (없으면 Optional.empty())
     */
    Optional<T> findByEventId(String eventId);

    /**
     * eventId로 이미 처리된 이벤트가 있는지 확인합니다.
     * 
     * @param eventId 이벤트 고유 ID
     * @return 이미 처리된 이벤트가 있으면 true
     */
    boolean existsByEventId(String eventId);
}

