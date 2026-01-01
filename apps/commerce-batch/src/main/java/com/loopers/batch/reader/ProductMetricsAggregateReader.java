package com.loopers.batch.reader;

import com.loopers.domain.dto.ProductRankingAggregation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

/**
 * 특정 기간 동안의 상품 지표를 집계하는 ItemReader.
 *
 * <p>이 Reader는 product_metrics 데이터를 가져와서 상품 ID별로 집계하며,
 * 특정 기간(주간 또는 월간) 동안의 데이터를 처리합니다. 집계된 결과에는
 * 가중치가 적용된 지표를 기반으로 계산된 랭킹 점수가 포함됩니다.
 *
 * <p>점수 계산 공식:
 * <pre>
 * 점수 = (조회수 * 0.1) +
 *       (좋아요수 * 0.2) +
 *       (주문수 * 0.6 * log10(판매금액 + 1))
 * </pre>
 */
@Slf4j
public class ProductMetricsAggregateReader implements ItemReader<ProductRankingAggregation> {

    private final EntityManager entityManager;
    private final String period;
    private final String periodType;
    private final int topN;
    private Iterator<ProductRankingAggregation> resultIterator;

    /**
     * ProductMetricsAggregateReader 생성자.
     *
     * @param entityManager JPA 엔티티 매니저
     * @param period 기간 문자열 (예: 주간 "2025-W01", 월간 "2025-01")
     * @param periodType 기간 타입 ("WEEKLY" 또는 "MONTHLY")
     * @param topN 가져올 최대 상위 랭킹 개수
     */
    public ProductMetricsAggregateReader(
        EntityManager entityManager,
        String period,
        String periodType,
        int topN
    ) {
        this.entityManager = entityManager;
        this.period = period;
        this.periodType = periodType;
        this.topN = topN;
    }

    @Override
    public ProductRankingAggregation read() {
        if (resultIterator == null) {
            resultIterator = fetchAggregatedData().iterator();
            log.info("Aggregated data fetched: period={}, type={}, count={}",
                period, periodType, resultIterator.hasNext() ? "available" : "empty");
        }

        return resultIterator.hasNext() ? resultIterator.next() : null;
    }

    /**
     * 설정된 기간 동안의 상품 지표를 가져와 집계합니다.
     *
     * @return 계산된 점수가 포함된 집계 랭킹 데이터 목록
     */
    private List<ProductRankingAggregation> fetchAggregatedData() {
        DateRange dateRange = calculateDateRange(period, periodType);

        String sql = """
            SELECT
                product_id,
                SUM(like_count) as total_likes,
                SUM(view_count) as total_views,
                SUM(order_count) as total_orders,
                SUM(sales_amount) as total_sales,
                (
                    SUM(view_count) * 0.1 +
                    SUM(like_count) * 0.2 +
                    SUM(order_count) * 0.6 * LOG10(SUM(sales_amount) + 1)
                ) as total_score
            FROM product_metrics
            WHERE created_at >= :startDate
              AND created_at < :endDate
            GROUP BY product_id
            ORDER BY total_score DESC
            LIMIT :topN
            """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("startDate", dateRange.start())
            .setParameter("endDate", dateRange.end())
            .setParameter("topN", topN);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return IntStream.range(0, results.size())
            .mapToObj(i -> {
                Object[] row = results.get(i);
                return new ProductRankingAggregation(
                    ((Number) row[0]).longValue(),
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(),
                    (BigDecimal) row[4],
                    i + 1
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 주어진 기간의 날짜 범위를 계산합니다.
     *
     * @param period 기간 문자열
     * @param type 기간 타입
     * @return 시작일과 종료일이 포함된 날짜 범위
     */
    private DateRange calculateDateRange(String period, String type) {
        if ("WEEKLY".equals(type)) {
            return calculateWeeklyDateRange(period);
        } else {
            return calculateMonthlyDateRange(period);
        }
    }

    /**
     * ISO 주차 형식에서 주간 날짜 범위를 계산합니다.
     *
     * @param yearWeek "YYYY-Wnn" 형식의 연-주차
     * @return 주간 날짜 범위
     */
    private DateRange calculateWeeklyDateRange(String yearWeek) {
        int year = Integer.parseInt(yearWeek.substring(0, 4));
        int week = Integer.parseInt(yearWeek.substring(6));

        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        LocalDate firstMonday = firstDayOfYear.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

        if (firstMonday.isAfter(firstDayOfYear)) {
            firstMonday = firstMonday.minusWeeks(1);
        }

        LocalDate startOfWeek = firstMonday.plusWeeks(week - 1);
        LocalDate endOfWeek = startOfWeek.plusWeeks(1);

        return new DateRange(startOfWeek, endOfWeek);
    }

    /**
     * 월간 날짜 범위를 계산합니다.
     *
     * @param yearMonth "YYYY-MM" 형식의 연-월
     * @return 월간 날짜 범위
     */
    private DateRange calculateMonthlyDateRange(String yearMonth) {
        LocalDate startOfMonth = LocalDate.parse(yearMonth + "-01");
        LocalDate endOfMonth = startOfMonth.plusMonths(1);

        return new DateRange(startOfMonth, endOfMonth);
    }

    /**
     * 시작일과 종료일을 포함하는 날짜 범위를 나타냅니다.
     *
     * @param start 시작일 (포함)
     * @param end 종료일 (미포함)
     */
    private record DateRange(LocalDate start, LocalDate end) {}
}
