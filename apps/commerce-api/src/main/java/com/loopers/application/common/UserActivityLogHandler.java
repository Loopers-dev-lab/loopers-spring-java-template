package com.loopers.application.common;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.loopers.domain.common.event.DomainEvent;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityLogHandler {

  private final Tracer tracer;

  @Async
  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void handle(DomainEvent event) {
    String traceId = getCurrentTraceId();

    log.info("[Activity:{}] traceId={} event={}",
        event.eventType(),
        traceId,
        event);

    // 전송할 땐 JSON 형태로 전달
  }

  private String getCurrentTraceId() {
    var currentSpan = tracer.currentSpan();
    if (currentSpan == null) {
      return "no-trace";
    }
    return currentSpan.context().traceId();
  }
}
