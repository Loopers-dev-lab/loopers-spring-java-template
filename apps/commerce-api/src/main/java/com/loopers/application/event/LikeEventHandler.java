package com.loopers.application.event;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.view.ProductListViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class LikeEventHandler {
  private final ProductListViewService productListViewService;
  private final LikeService likeService;

  private final ConcurrentMap<Long, Long> lastProcessedTime = new ConcurrentHashMap<>();

  @Value("${like.aggregation.debounce-interval:5000}")
  private long debounceInterval;

  @EventListener
  @Async
  public void handleLikeEvent(LikeEvent event) {
    log.debug("좋아요 이벤트 수신 - 사용자ID: {}, 상품ID: {}, 액션: {}",
        event.userId(), event.productId(), event.action());

    Long lastTime = lastProcessedTime.get(event.productId());
    long currentTime = System.currentTimeMillis();

    if (lastTime != null && (currentTime - lastTime) < debounceInterval) {
      log.debug("디바운싱으로 인한 이벤트 무시 - 상품ID: {}, 마지막처리: {}ms 전",
          event.productId(), currentTime - lastTime);
      return;
    }

    try {
      long actualLikeCount = likeService.getLikeCount(event.productId());
      productListViewService.syncLikeCount(event.productId(), actualLikeCount);
      lastProcessedTime.put(event.productId(), currentTime);

      log.info("ProductListView 좋아요 집계 동기화 완료 - 상품ID: {}, 실제수: {}, 액션: {}",
          event.productId(), actualLikeCount, event.action());
    } catch (Exception e) {
      log.error("ProductListView 좋아요 집계 동기화 실패 - 상품ID: {}, 액션: {}",
          event.productId(), event.action(), e);
    }
  }

  @Scheduled(fixedRate = 3600000)
  public void cleanupOldEntries() {
    long currentTime = System.currentTimeMillis();
    long cleanupThreshold = 3600000;

    lastProcessedTime.entrySet().removeIf(entry ->
        (currentTime - entry.getValue()) > cleanupThreshold
    );

    log.debug("오래된 디바운싱 항목 정리 완료 - 남은 항목: {}", lastProcessedTime.size());
  }
}
