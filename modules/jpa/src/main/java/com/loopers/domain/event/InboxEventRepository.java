package com.loopers.domain.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {
    boolean existsByMessageId(String messageId);
    Optional<InboxEvent> findByMessageId(String messageId);
}

