package com.loopers.batch.reader;

import com.loopers.batch.dto.ProductMetricsAgg;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductMetricReader {
    private final DataSource dataSource;

    public JdbcPagingItemReader<ProductMetricsAgg> reader(
            String startDate,
            String endDate,
            String readerName
    ) throws Exception {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("start", startDate);
        params.put("end", endDate);

        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("""
            SELECT
              ref_product_id,
              SUM(COALESCE(like_count, 0))   AS sum_like,
              SUM(COALESCE(view_count, 0))   AS sum_view,
              SUM(COALESCE(sales_volume, 0)) AS sum_sales
            """);
        provider.setFromClause("FROM product_metrics");
        provider.setWhereClause("WHERE metric_date BETWEEN :start AND :end");
        provider.setGroupClause("GROUP BY ref_product_id");

        Map<String, org.springframework.batch.item.database.Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("ref_product_id", org.springframework.batch.item.database.Order.ASCENDING);
        provider.setSortKeys(sortKeys);

        return new JdbcPagingItemReaderBuilder<ProductMetricsAgg>()
                .name(readerName)
                .dataSource(dataSource)
                .queryProvider(provider.getObject())
                .parameterValues(params)
                .pageSize(1000)
                .rowMapper((ResultSet rs, int rowNum) -> new ProductMetricsAgg(
                        rs.getLong("ref_product_id"),
                        rs.getLong("sum_like"),
                        rs.getLong("sum_view"),
                        rs.getLong("sum_sales")
                ))
                .build();
    }
}
