package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductMetrics를 읽기 위한 Spring Batch ItemReader Factory.
 * <p>
 * Chunk-Oriented Processing을 위해 JPA Repository 기반 Reader를 생성합니다.
 * 특정 날짜의 product_metrics 데이터를 페이징하여 읽습니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>대량 데이터를 메모리 효율적으로 처리하기 위해 페이징 방식 사용</li>
 *   <li>날짜 파라미터를 받아 해당 날짜의 데이터만 조회</li>
 *   <li>product_id 기준 정렬로 일관된 읽기 순서 보장</li>
 * </ul>
 * </p>
 * <p>
 * <b>DIP 준수:</b>
 * <ul>
 *   <li>도메인 레이어의 ProductMetricsRepository 인터페이스를 사용</li>
 *   <li>Spring Batch의 기술적 제약으로 인해 getJpaRepository()를 통해 JPA Repository 접근</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsItemReader {

    private final ProductMetricsRepository productMetricsRepository;

    /**
     * ProductMetrics를 읽는 ItemReader를 생성합니다.
     * <p>
     * Job 파라미터에서 날짜를 받아 해당 날짜의 데이터만 조회합니다.
     * </p>
     *
     * @param targetDate 조회할 날짜 (yyyyMMdd 형식)
     * @return RepositoryItemReader 인스턴스
     */
    public RepositoryItemReader<ProductMetrics> createReader(String targetDate) {
        // 날짜 파라미터 파싱
        LocalDate date = parseDate(targetDate);
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        log.info("ProductMetrics Reader 초기화: targetDate={}, startDateTime={}, endDateTime={}", 
            date, startDateTime, endDateTime);

        // 정렬 기준 설정 (product_id 기준 오름차순)
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("productId", Sort.Direction.ASC);

        // Spring Batch의 RepositoryItemReader는 PagingAndSortingRepository를 직접 요구하므로
        // 기술적 제약으로 인해 getJpaRepository()를 통해 접근
        PagingAndSortingRepository<ProductMetrics, Long> jpaRepository = 
            productMetricsRepository.getJpaRepository();
        
        return new RepositoryItemReaderBuilder<ProductMetrics>()
            .name("productMetricsReader")
            .repository(jpaRepository)
            .methodName("findByUpdatedAtBetween")
            .arguments(startDateTime, endDateTime)
            .pageSize(100) // Chunk 크기와 동일하게 설정
            .sorts(sorts)
            .build();
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱합니다.
     * <p>
     * yyyyMMdd 형식의 문자열을 파싱하며, 파싱 실패 시 오늘 날짜를 반환합니다.
     * </p>
     *
     * @param dateStr 날짜 문자열 (yyyyMMdd 형식)
     * @return 파싱된 날짜
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            log.warn("날짜 파라미터가 없어 오늘 날짜를 사용합니다.");
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}, 오늘 날짜를 사용합니다.", dateStr, e);
            return LocalDate.now();
        }
    }
}

