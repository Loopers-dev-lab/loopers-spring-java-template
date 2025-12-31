package com.loopers.batch.reader;

import com.loopers.domain.dto.ProductRankingAggregation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ItemReader for aggregating product metrics over a time period.
 *
 * <p>This reader fetches product_metrics data and aggregates it by product ID
 * for a specific time period (weekly or monthly). The aggregated results include
 * calculated ranking scores based on weighted metrics.
 *
 * <p>Scoring formula:
 * <pre>
 * score = (viewCount * 0.1) +
 *         (likeCount * 0.2) +
 *         (orderCount * 0.6 * log10(salesAmount + 1))
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
     * Constructs a new ProductMetricsAggregateReader.
     *
     * @param entityManager the JPA entity manager
     * @param period the period string (e.g., "2025-W01" for weekly, "2025-01" for monthly)
     * @param periodType the type of period ("WEEKLY" or "MONTHLY")
     * @param topN the maximum number of top rankings to fetch
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
     * Fetches and aggregates product metrics for the configured period.
     *
     * @return list of aggregated ranking data with calculated scores
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
     * Calculates the date range for the given period.
     *
     * @param period the period string
     * @param type the period type
     * @return the date range with start and end dates
     */
    private DateRange calculateDateRange(String period, String type) {
        if ("WEEKLY".equals(type)) {
            return calculateWeeklyDateRange(period);
        } else {
            return calculateMonthlyDateRange(period);
        }
    }

    /**
     * Calculates weekly date range from ISO week format.
     *
     * @param yearWeek the year-week in format "YYYY-Wnn"
     * @return date range for the week
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
     * Calculates monthly date range.
     *
     * @param yearMonth the year-month in format "YYYY-MM"
     * @return date range for the month
     */
    private DateRange calculateMonthlyDateRange(String yearMonth) {
        LocalDate startOfMonth = LocalDate.parse(yearMonth + "-01");
        LocalDate endOfMonth = startOfMonth.plusMonths(1);

        return new DateRange(startOfMonth, endOfMonth);
    }

    /**
     * Represents a date range with start and end dates.
     *
     * @param start the start date (inclusive)
     * @param end the end date (exclusive)
     */
    private record DateRange(LocalDate start, LocalDate end) {}
}
