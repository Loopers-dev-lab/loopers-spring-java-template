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

    @Transactional(readOnly = true)
    public List<RankingInfo> getRankingItems(String date, int page, int size) {
        return rankingService.getRankingRows(date, page, size)
                .stream()
                .map(this::toDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long count(String date) {
        return rankingService.count(date);
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

        Product product = productService.getProduct(row.productId());
        ProductInfo productInfo = ProductInfo.of(product, brandService.getBrand(product.getBrandId()));
        return new RankingInfo(row.rank(), row.score(), productInfo);
    }
}
