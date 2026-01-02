package com.loopers.batch.job.productRankingJob.step.reader;


import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.batch.item.ItemReader;
import com.loopers.domain.rank.ProductRankingAggregation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class RankingScoreReader implements ItemReader<ProductRankingAggregation> {

    private final EntityManager entityManager;
    private final String anchorDate;
    private final String periodType;

    private Iterator<ProductRankingAggregation> iterator;

    public RankingScoreReader(
            EntityManager entityManager,
            String anchorDate,
            String periodType
    ) {
        this.entityManager = entityManager;
        this.anchorDate = anchorDate;
        this.periodType = periodType;
    }

    @Override
    public ProductRankingAggregation read() {
        if (iterator == null) {
            iterator = fetch().iterator();
        }

        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<ProductRankingAggregation> fetch() {
        LocalDate endDate = LocalDate.parse(anchorDate);
        int window = "WEEKLY".equalsIgnoreCase(periodType) ? 6 : 29;
        LocalDate startDate = endDate.minusDays(window);

        String sql =
                "SELECT prd.product_id, " +
                "       SUM(COALESCE(prd.like_count, 0)) AS like_count, " +
                "       SUM(COALESCE(prd.view_count, 0)) AS view_count, " +
                "       SUM(COALESCE(prd.order_count, 0)) AS order_count, " +
                "       SUM(COALESCE(prd.sales_amount, 0)) AS sales_amount " +
                "  FROM product_ranking_daily prd " +
                " WHERE prd.stat_date BETWEEN :start AND :end " +
                " GROUP BY prd.product_id";

        Query q = entityManager.createNativeQuery(sql);
        q.setParameter("start", startDate);
        q.setParameter("end", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();

        // 집계값 기반으로 점수 계산 후 정렬 및 순위 부여 (일간과 동일 가중치)
        List<Row> temp = new ArrayList<>();
        for (Object[] r : rows) {
            Long productId = toLong(r[0]);
            Integer likeCount = toInt(r[1]);
            Integer viewCount = toInt(r[2]);
            Integer orderCount = toInt(r[3]);
            BigDecimal salesAmount = toDecimal(r[4]);
            double score = calcScore(viewCount, likeCount, orderCount, salesAmount);
            temp.add(new Row(productId, likeCount, viewCount, orderCount, salesAmount, score));
        }

        temp.sort(Comparator.comparingDouble((Row x) -> x.score).reversed());

        List<ProductRankingAggregation> result = new ArrayList<>(temp.size());
        int rank = 1;
        for (Row r : temp) {
            result.add(new ProductRankingAggregation(
                    r.productId,
                    r.likeCount,
                    r.viewCount,
                    r.orderCount,
                    r.salesAmount,
                    rank++
            ));
        }
        return result;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    private static double calcScore(Integer viewCount, Integer likeCount, Integer orderCount, BigDecimal salesAmount) {
        int view = viewCount == null ? 0 : viewCount;
        int like = likeCount == null ? 0 : likeCount;
        int orders = orderCount == null ? 0 : orderCount;
        BigDecimal amount = salesAmount == null ? BigDecimal.ZERO : salesAmount;
        double orderBase = amount.signum() > 0 ? amount.doubleValue() : (double) orders;
        return (0.1d * view) + (0.2d * like) + (0.6d * orderBase);
    }

    private static class Row {
        final Long productId;
        final Integer likeCount;
        final Integer viewCount;
        final Integer orderCount;
        final BigDecimal salesAmount;
        final double score;

        Row(Long productId, Integer likeCount, Integer viewCount, Integer orderCount, BigDecimal salesAmount, double score) {
            this.productId = productId;
            this.likeCount = likeCount;
            this.viewCount = viewCount;
            this.orderCount = orderCount;
            this.salesAmount = salesAmount;
            this.score = score;
        }
    }
}
