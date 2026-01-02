package com.loopers.batch.processor;

import com.loopers.batch.dto.ProductMetricsAgg;
import com.loopers.batch.dto.ProductRankRow;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@StepScope
public class ProductMetricProcessor implements ItemProcessor<ProductMetricsAgg, ProductRankRow> {
    private static final BigDecimal W_VIEW = new BigDecimal("0.1");
    private static final BigDecimal W_LIKE = new BigDecimal("0.3");
    private static final BigDecimal W_SALES = new BigDecimal("0.6");

    @Override
    public ProductRankRow process(ProductMetricsAgg item) {
        BigDecimal score =
                BigDecimal.valueOf(item.sumView()).multiply(W_VIEW)
                        .add(BigDecimal.valueOf(item.sumLike()).multiply(W_LIKE))
                        .add(BigDecimal.valueOf(item.sumSales()).multiply(W_SALES))
                        .setScale(4, RoundingMode.HALF_UP);

        return new ProductRankRow(
                item.productId(),
                item.sumView(),
                item.sumLike(),
                item.sumSales(),
                score
        );
    }
}
