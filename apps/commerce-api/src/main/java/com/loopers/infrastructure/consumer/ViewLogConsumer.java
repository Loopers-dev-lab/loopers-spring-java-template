package com.loopers.infrastructure.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.product.ProductViewLog;
import com.loopers.domain.product.ProductViewLogRepository;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.infrastructure.ranking.RankingRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewLogConsumer {

    private final ProductViewLogRepository viewLogRepository;
    private final RankingRedisService rankingRedisService;

    @KafkaListener(
            topics = "product-viewed",
            groupId = "view-log-consumer",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consumeBatch(List<ProductViewedEvent> events, Acknowledgment ack) {
        if (events.isEmpty()) {
            ack.acknowledge();
            return;
        }

        try {
            // 1. MySQL 저장 (원본 보존)
            List<ProductViewLog> logs = events.stream()
                    .map(event -> ProductViewLog.create(event.getProductId()))
                    .toList();
            viewLogRepository.saveAll(logs);

            // 2. Redis 업데이트 (상품별로 그룹핑해서 한 번에)
            LocalDate today = LocalDate.now();
            Map<Long, Long> viewCountByProduct = events.stream()
                    .collect(Collectors.groupingBy(
                            ProductViewedEvent::getProductId,
                            Collectors.counting()
                    ));

            for (Map.Entry<Long, Long> entry : viewCountByProduct.entrySet()) {
                Long productId = entry.getKey();
                int count = entry.getValue().intValue();
                rankingRedisService.incrementScoreForView(today, productId, count);
            }

            ack.acknowledge();
            log.info("조회 로그 배치 처리 완료: {}건, 상품 {}종", events.size(), viewCountByProduct.size());
        } catch (Exception e) {
            log.error("조회 로그 배치 처리 실패: {}건", events.size(), e);
            // ack 안 하면 재처리됨
        }
    }
}
