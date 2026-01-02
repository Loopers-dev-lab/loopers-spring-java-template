package com.loopers.application.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.ranking.RankingRow;
import com.loopers.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingFacade {

    private final RankingService rankingService;
    private final ProductService productService;
    private final BrandService brandService;
    private final RankingMaterializedViewService rankingMaterializedViewService;

    @Transactional(readOnly = true)
    public List<RankingInfo> getRankingItems(String date, RankingPeriod period, int page, int size) {
        if (period.isDaily()) {
            return rankingService.getRankingRows(date, page, size)
                    .stream()
                    .map(this::toDto)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return rankingMaterializedViewService.getRankings(period, period.resolveKey(date), page, size)
                .stream()
                .map(this::toSnapshotDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long count(String date, RankingPeriod period) {
        if (period.isDaily()) {
            return rankingService.count(date);
        }
        return rankingMaterializedViewService.count(period, period.resolveKey(date));
    }

    @Transactional(readOnly = true)
    public Long findRank(Long productId, String date) {
        return rankingService.findRank(productId, date);
    }

    public void recordView(Long productId) {
        if (productId == null) {
            return;
        }
        rankingService.recordViewEvent(productId, null);
    }

    private RankingInfo toDto(RankingRow row) {
        if (row.productId() == null) {
            return null;
        }

        return createRankingInfo(row.productId(), row.rank(), row.score());
    }

    private RankingInfo toSnapshotDto(ProductRankSnapshot snapshot) {
        if (snapshot.productId() == null) {
            return null;
        }
        return createRankingInfo(snapshot.productId(), snapshot.rank(), snapshot.score());
    }

    private RankingInfo createRankingInfo(Long productId, long rank, double score) {
        Product product = productService.getProduct(productId);
        ProductInfo productInfo = ProductInfo.of(product, brandService.getBrand(product.getBrandId()));
        return new RankingInfo(rank, score, productInfo);
    }
}
