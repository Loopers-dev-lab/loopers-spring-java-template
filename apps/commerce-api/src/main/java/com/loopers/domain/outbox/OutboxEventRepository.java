package com.loopers.domain.outbox;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {

  void save(OutboxEvent event);

  Optional<OutboxEvent> findById(String eventId);

  // 전송 준비된 NEW 상태 이벤트를 limit개 조회
  List<OutboxEvent> findNewEventsReadyToSend(Instant now, int limit);

  // 만료된 이벤트를 NEW로 복구
  int recoverExpiredEvents(Instant now);

  // 이벤트 상태를 SENDING으로 변경하고 리스 만료 시간 설정
  int updateStatusToSending(String eventId, Instant leaseExpiry);

  // 이벤트 상태를 NEW로 되돌림 (즉시 발행 실패 시)
  void resetToNew(String eventId);
}
