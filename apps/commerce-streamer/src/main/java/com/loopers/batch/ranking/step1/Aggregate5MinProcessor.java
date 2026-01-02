package com.loopers.batch.ranking.step1;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.batch.ranking.dto.ProductScore5MinDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Step 1 Processor: RankingEventLog를 ProductScore5MinDto로 변환
 * 5분 구간 계산 및 Raw Metrics 추출
 */
@Slf4j
@Component
public class Aggregate5MinProcessor implements ItemProcessor<RankingEventLog, ProductScore5MinDto> {

    @Override
    public ProductScore5MinDto process(RankingEventLog log) throws Exception {
        if (log == null) {
            return null;
        }

        // 5분 구간 계산 (occurredAt을 5분 단위로 내림)
        LocalDateTime[] interval = calculate5MinInterval(log.getOccurredAt());
        LocalDateTime startTime = interval[0];
        LocalDateTime endTime = interval[1];

        // Raw Metrics 추출
        RankingEventType eventType = log.getEventType();
        Metrics metrics = extractMetrics(log, eventType);

        return ProductScore5MinDto.builder()
            .productId(log.getProductId())
            .startTime(startTime)
            .endTime(endTime)
            .orderAmountSum(metrics.orderAmountSum())
            .likeCount(metrics.likeCount())
            .viewCount(metrics.viewCount())
            .build();
    }

    /**
     * 5분 구간 계산 (occurredAt을 5분 단위로 내림)
     */
    private LocalDateTime[] calculate5MinInterval(LocalDateTime occurredAt) {
        LocalDateTime startTime = occurredAt.truncatedTo(ChronoUnit.MINUTES)
            .withMinute((occurredAt.getMinute() / 5) * 5)
            .withSecond(0)
            .withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(5);
        return new LocalDateTime[]{startTime, endTime};
    }

    /**
     * Raw Metrics 추출
     */
    private Metrics extractMetrics(RankingEventLog log, RankingEventType eventType) {
        return switch (eventType) {
            case ORDER -> {
                BigDecimal orderAmountSum = (log.getRawPrice() != null && log.getRawQuantity() != null)
                    ? log.getRawPrice().multiply(BigDecimal.valueOf(log.getRawQuantity()))
                    : BigDecimal.ZERO;
                yield new Metrics(orderAmountSum, 0L, 0L);
            }
            case LIKE -> new Metrics(BigDecimal.ZERO, 1L, 0L);
            case VIEW -> new Metrics(BigDecimal.ZERO, 0L, 1L);
        };
    }

    /**
     * Raw Metrics를 담는 레코드
     */
    private record Metrics(BigDecimal orderAmountSum, Long likeCount, Long viewCount) {}
}

