package com.loopers.batch.ranking.step1;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import com.loopers.infrastructure.ranking.RankingEventLogJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Step 1 Reader: RankingEventLog에서 5분 구간 데이터 조회
 * Late-Arriving Data를 위해 2분 버퍼 적용 (현재 시간 기준 2분 전까지)
 * 
 * Spring Batch 표준 패턴: ItemReader는 단일 아이템을 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Aggregate5MinReader implements ItemReader<RankingEventLog> {

    private final RankingEventLogJpaRepository rankingEventLogJpaRepository;
    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;

    private Iterator<RankingEventLog> iterator;
    private boolean initialized = false;

    @Override
    public RankingEventLog read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!initialized) {
            initialize();
            initialized = true;
        }

        if (iterator == null || !iterator.hasNext()) {
            return null;
        }

        return iterator.next();
    }

    private void initialize() {
        // 1. last_processed_time 파악 (최근 30일 내)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        Optional<LocalDateTime> maxEndTime = productScore5MinJpaRepository.findMaxEndTimeAfter(cutoffDate);
        
        LocalDateTime lastProcessedTime = maxEndTime.orElse(cutoffDate);
        
        // 2. target_time 계산 (현재 시간 기준 2분 전 - Late-Arriving Data 버퍼)
        LocalDateTime targetTime = LocalDateTime.now().minusMinutes(2);
        
        // 3. last_processed_time부터 target_time까지의 데이터 조회
        if (lastProcessedTime.isAfter(targetTime) || lastProcessedTime.equals(targetTime)) {
            log.info("No new data to process. lastProcessedTime: {}, targetTime: {}", lastProcessedTime, targetTime);
            iterator = null;
            return;
        }

        // 4. 누락된 구간을 5분 단위로 나누어 각각 처리 (Catch-up 로직)
        List<RankingEventLog> allLogs = new ArrayList<>();
        
        // 5분 단위로 구간 계산
        LocalDateTime currentStart = truncateTo5MinuteInterval(lastProcessedTime);
        
        while (currentStart.isBefore(targetTime)) {
            LocalDateTime currentEnd = currentStart.plusMinutes(5);
            
            // targetTime을 넘지 않도록 조정
            if (currentEnd.isAfter(targetTime)) {
                currentEnd = targetTime;
            }
            
            // 각 5분 구간별로 RankingEventLog 조회
            List<RankingEventLog> intervalLogs = rankingEventLogJpaRepository.findByOccurredAtBetween(
                currentStart, currentEnd
            );
            
            allLogs.addAll(intervalLogs);
            
            log.debug("Aggregate5MinReader: Found {} events in interval {} ~ {}", 
                intervalLogs.size(), currentStart, currentEnd);
            
            // 다음 5분 구간으로 이동
            currentStart = currentEnd;
        }

        log.info("Aggregate5MinReader: Found {} total events between {} and {} (processed in {} intervals)", 
            allLogs.size(), lastProcessedTime, targetTime, 
            (int) Math.ceil(Duration.between(lastProcessedTime, targetTime).toMinutes() / 5.0));
        
        iterator = allLogs.isEmpty() ? null : allLogs.iterator();
    }

    /**
     * 시간을 5분 단위로 내림 처리
     */
    private LocalDateTime truncateTo5MinuteInterval(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.MINUTES)
            .withMinute((time.getMinute() / 5) * 5)
            .withSecond(0)
            .withNano(0);
    }
}

